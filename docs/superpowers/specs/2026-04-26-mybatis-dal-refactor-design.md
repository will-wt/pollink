# MyBatis + dal 包结构重构设计文档

## 目标

将项目中所有直接写 SQL（JdbcTemplate）的地方替换为 MyBatis Mapper，并把与数据访问相关的实体类和 Mapper 接口统一放置在 `dal` 包名下。

## 当前状态

| 模块 | 当前数据访问方式 | 当前包结构 |
|------|----------------|----------|
| `pollink-server` | MyBatis Mapper（`@Mapper`）已存在 | `domain/entity/` + `infrastructure/repository/` |
| `pollink-admin` | JdbcTemplate（4 个 Controller 直接写 SQL） | Controller 中内联 SQL |
| `pollink-discovery` | JdbcTemplate（`MysqlDiscoveryService`） | `model/ServerNode` |

## 方案：简单平移（方案 A）

各模块内部统一为 `dal.mapper`（Mapper 接口）和 `dal.entity`（实体类）两个子包。保留现有业务逻辑不变，只做包结构迁移和访问方式替换。

### 包结构统一规范

```
com.nova.pollink.<module>
├── dal/
│   ├── entity/     # 数据库实体（PO）
│   └── mapper/     # MyBatis Mapper 接口
```

### 模块 1：pollink-server

server 已有 MyBatis Mapper，但需要把 `domain/entity/` 和 `infrastructure/repository/` 合并到 `dal/`。

**迁移路径：**

| 原路径 | 新路径 | 操作 |
|--------|--------|------|
| `domain/entity/Message.java` | `dal/entity/Message.java` | 移动 |
| `domain/entity/Config.java` | `dal/entity/Config.java` | 移动 |
| `infrastructure/repository/MessageMapper.java` | `dal/mapper/MessageMapper.java` | 移动 |
| `infrastructure/repository/ConfigMapper.java` | `dal/mapper/ConfigMapper.java` | 移动 |
| `domain/repository/MessageRepository.java` | `dal/repository/MessageRepository.java` | 移动（可选保留） |
| `domain/repository/ConfigRepository.java` | `dal/repository/ConfigRepository.java` | 移动（可选保留） |
| `infrastructure/repository/MessageRepositoryImpl.java` | `dal/repository/MessageRepositoryImpl.java` | 移动（可选保留） |
| `infrastructure/repository/ConfigRepositoryImpl.java` | `dal/repository/ConfigRepositoryImpl.java` | 移动（可选保留） |

**说明：**
- `MessageRepository` / `ConfigRepository` 接口保留，作为领域层对数据访问的契约。
- `MessageRepositoryImpl` / `ConfigRepositoryImpl` 保留，实现仍调用 Mapper。
- `MessageService` / `ConfigService` 注入 `MessageRepository` / `ConfigRepository`，不变。
- `ServerNode` 实体：server 本身不直接操作 `server_nodes` 表（通过 discovery SPI），无需迁移。

### 模块 2：pollink-admin

admin 所有 Controller 当前使用 JdbcTemplate 直接写 SQL，需全部改为注入 MyBatis Mapper。

**新建文件：**

| 文件 | 说明 |
|------|------|
| `dal/entity/MessageEntity.java` | messages 表实体 |
| `dal/entity/ConfigEntity.java` | configs 表实体 |
| `dal/entity/GrayRuleEntity.java` | gray_rules 表实体 |
| `dal/mapper/MessageMapper.java` | messages CRUD |
| `dal/mapper/ConfigMapper.java` | configs CRUD |
| `dal/mapper/GrayRuleMapper.java` | gray_rules CRUD |

**Controller 改动：**

| Controller | 当前 | 改为 |
|------------|------|------|
| `MessageController` | `JdbcTemplate` | 注入 `MessageMapper` |
| `ConfigController` | `JdbcTemplate` | 注入 `ConfigMapper` |
| `GrayRuleController` | `JdbcTemplate` | 注入 `GrayRuleMapper` |
| `NodeController` | `JdbcTemplate`（维护模式） | 注入 `ServerNodeMapper`（或保留 DiscoveryService） |

**NodeController 特殊处理：**
- `listNodes()` 通过 `DiscoveryService.listActiveNodes()` 获取，不直接访问数据库，无需改动。
- `setMaintenance()` 需要更新 `server_nodes` 表，要么保留 JdbcTemplate（简单），要么新建 `ServerNodeMapper`。
- **建议：** 为一致性，新建 `ServerNodeMapper.updateStatus()`。

### 模块 3：pollink-discovery

discovery 的 `MysqlDiscoveryService` 使用 JdbcTemplate 直接操作 `server_nodes` 表，需改为 Mapper。

**新建文件：**

| 文件 | 说明 |
|------|------|
| `dal/entity/ServerNodeEntity.java` | server_nodes 表实体（从 `model/ServerNode` 迁移） |
| `dal/mapper/ServerNodeMapper.java` | server_nodes CRUD + 心跳查询 |

**改动：**
- `model/ServerNode.java` 移到 `dal/entity/ServerNodeEntity.java`。
- `MysqlDiscoveryService` 注入 `ServerNodeMapper`，替换所有 `JdbcTemplate` 调用。
- `NacosDiscoveryService` 不涉及数据库，无需改动。
- `ServerNode` 同时作为 API 返回模型：保留 `model/ServerNode` 作为 DTO，`dal/entity/ServerNodeEntity` 作为 PO。为简化，也可以合并为同一个类放在 `dal/entity/`。
- **建议：** 合并为一个 `ServerNode` 放在 `dal/entity/`，`model/` 包删除。

## 依赖调整

### pollink-admin pom.xml
当前 admin POM 只有 `spring-boot-starter-web` + `spring-boot-starter-jdbc`（通过 discovery 传递），需显式添加 `mybatis-spring-boot-starter`：

```xml
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
</dependency>
```

### pollink-discovery pom.xml
已有 `spring-boot-starter-jdbc`，改为 `mybatis-spring-boot-starter`：

```xml
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
</dependency>
```

## SQL 映射规则

所有内联 SQL 转换为 `@Select` / `@Insert` / `@Update` / `@Delete` 注解：

- `JdbcTemplate.queryForList(sql)` → `@Select` + 返回 `List<Entity>`
- `JdbcTemplate.update(sql, params)` → `@Insert` / `@Update` / `@Delete`
- `JdbcTemplate.query(sql, rowMapper)` → `@Select` + resultMap 或驼峰映射

## 回滚策略

纯代码结构迁移，无数据库 schema 变更。回滚时恢复旧 package 的 import 即可。
