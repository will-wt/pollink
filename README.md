# Pollink

Pollink 是一个轻量级后端长轮询（Long Polling）框架，支持集群部署。它通过持久化 HTTP 连接实现服务端向客户端的实时消息推送和配置下发，无需客户端维护 WebSocket 连接，适合需要低延迟、高可靠消息投递的场景。

## 功能清单

- **长轮询消息推送**：客户端通过 HTTP 长连接挂起请求，服务端有新消息时立即响应，实现准实时推送
- **配置中心**：支持 Key-Value 配置的版本化管理与灰度发布，客户端按版本号增量同步
- **集群广播**：多节点间通过 gRPC 双向流建立持久连接，数据变更自动广播到所有节点
- **服务发现**：基于 MySQL 的轻量级节点注册与心跳机制，支持节点上下线自动感知
- **优雅关闭**：支持请求排空、挂起连接唤醒、节点注销的完整关闭流程
- **管理后台**：提供 Web UI 和 REST API，支持消息发送、配置管理、节点监控、灰度规则管理

## 模块结构

```
nova-long-pulling/
├── pollink-discovery    # 服务发现 SPI（MySQL / Nacos 实现）
├── pollink-server       # 核心长轮询服务端（HTTP 8080 + gRPC 9101）
├── pollink-client       # 纯 Java HTTP 客户端（无 Spring 依赖）
├── pollink-admin        # 管理后台 Web UI + REST API（8090）
├── pollink-example      # 演示启动器（内置 Server + Client）
└── sql/
    └── init.sql         # MySQL 数据库初始化脚本
```

| 模块 | 职责 | 端口 |
|------|------|------|
| `pollink-discovery` | 服务发现接口与实现 | — |
| `pollink-server` | 长轮询核心、集群通信、数据推送 | 8080, 9101 |
| `pollink-client` | Java 客户端 SDK | — |
| `pollink-admin` | 管理界面与运维 API | 8090 |
| `pollink-example` | 快速体验 Demo | — |

## 技术栈

- **Java 17**
- **Spring Boot 3.2.0**（server / admin / example）
- **MyBatis 3.0.3**（注解式 Mapper）
- **gRPC 1.59.0** + **Protobuf 3.25.0**（节点间通信）
- **MySQL 8.x**（数据持久化、服务发现）
- **Maven**（多模块构建）

## 技术架构

### 长轮询核心

客户端向 `/api/v1/poll/messages` 和 `/api/v1/poll/configs` 发起请求，服务端使用 `DeferredResult` 将连接挂起最多 30 秒。新数据到达时，服务端立即响应，客户端收到结果后重新发起轮询，形成持续的数据拉取循环。

### 集群通信

多节点通过 MySQL `server_nodes` 表互相发现，节点间建立持久的 gRPC 双向流连接。当任一节点收到推送请求时，通过 gRPC 广播通知其他节点，各节点唤醒本地的挂起连接，实现集群级别的消息覆盖。

### 客户端 SDK

`LongPollingClient` 使用 `java.net.http.HttpClient` 发起轮询，内置指数退避重试（1s → 2s → 4s → 8s，上限 30s）和 503 退避机制。支持通过 Builder 配置订阅 Topic、消息处理器和配置处理器。

## 快速开始

### 1. 初始化数据库

```bash
mysql -u root -p < sql/init.sql
```

数据库名：`nova_long_polling`

### 2. 编译项目

```bash
mvn install -DskipTests
```

### 3. 启动服务端

```bash
cd pollink-server
mvn spring-boot:run
```

服务端启动后自动向数据库注册节点，并监听 HTTP 8080 和 gRPC 9101。

### 4. 启动管理后台（可选）

```bash
cd pollink-admin
mvn spring-boot:run
```

打开 http://localhost:8090 访问管理界面，可进行消息发送、配置发布、节点管理等操作。

### 5. 运行示例

```bash
cd pollink-example
mvn spring-boot:run
```

示例模块会同时启动一个 Server 和一个 Client，Client 订阅 `demo_topic` 并打印收到的消息和配置。

### 6. 使用客户端 SDK

```java
LongPollingClient client = LongPollingClientBuilder.builder()
    .serverUrl("http://localhost:8080")
    .clientId("my-client-001")
    .pollIntervalSeconds(2)
    .subscribeTopics("order_notify", "system_alert")
    .messageHandler(msg -> {
        System.out.println("收到消息: " + msg.getPayload());
    })
    .configHandler(cfg -> {
        System.out.println("配置更新: " + cfg.getKey() + " = " + cfg.getValue());
    })
    .build();

client.start();
```

## 配置项

### pollink-server

```yaml
nova:
  pollink:
    server:
      grpc-port: 9101                  # gRPC 节点通信端口
      poll-timeout-seconds: 30         # 长轮询挂起超时
      node-ip: ""                      # 显式指定节点 IP（留空则自动检测）
    discovery:
      heartbeat-interval-seconds: 5    # 心跳间隔
      node-timeout-seconds: 15         # 节点超时判定
```

### pollink-admin

```yaml
nova:
  pollink:
    admin:
      server-url: http://localhost:8080  # Server 推送 API 地址
```

## 数据库表

- `messages` — 消息表（topic、payload、状态、过期时间）
- `configs` — 配置表（key-value、版本号、发布状态）
- `server_nodes` — 节点注册表（IP、状态、心跳时间、连接数）
- `gray_rules` — 灰度规则表（消息/配置的灰度发布条件）
