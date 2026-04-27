# MyBatis + dal 包结构重构实现计划2

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将项目中所有 JdbcTemplate 直接写 SQL 的地方替换为 MyBatis Mapper，并将实体类和 Mapper 统一放到各模块的 `dal` 包下。

**Architecture:** 各模块内部统一为 `dal.entity`（实体）和 `dal.mapper`（Mapper）两个子包。server 保留 Repository 接口和实现，移到 `dal.repository`。admin 和 discovery 的 Controller/Service 直接注入 Mapper。

**Tech Stack:** Java 17, Spring Boot 3.x, MyBatis, Maven

---

## 文件结构总览

### 新建文件

```
pollink-discovery/
  dal/mapper/ServerNodeMapper.java
  dal/entity/ServerNode.java          (从 model/ServerNode.java 复制)

pollink-admin/
  dal/entity/MessageEntity.java
  dal/entity/ConfigEntity.java
  dal/entity/GrayRuleEntity.java
  dal/entity/ServerNodeEntity.java
  dal/mapper/MessageMapper.java
  dal/mapper/ConfigMapper.java
  dal/mapper/GrayRuleMapper.java
  dal/mapper/ServerNodeMapper.java
```

### 移动文件（server）

```
pollink-server/
  domain/entity/Message.java              → dal/entity/Message.java
  domain/entity/Config.java               → dal/entity/Config.java
  infrastructure/repository/MessageMapper.java     → dal/mapper/MessageMapper.java
  infrastructure/repository/ConfigMapper.java      → dal/mapper/ConfigMapper.java
  domain/repository/MessageRepository.java         → dal/repository/MessageRepository.java
  domain/repository/ConfigRepository.java          → dal/repository/ConfigRepository.java
  infrastructure/repository/MessageRepositoryImpl.java → dal/repository/MessageRepositoryImpl.java
  infrastructure/repository/ConfigRepositoryImpl.java  → dal/repository/ConfigRepositoryImpl.java
```

### 修改的文件

- `pollink-discovery/pom.xml`
- `pollink-discovery/src/main/java/com/nova/pollink/discovery/DiscoveryService.java`
- `pollink-discovery/src/main/java/com/nova/pollink/discovery/impl/MysqlDiscoveryService.java`
- `pollink-discovery/src/main/java/com/nova/pollink/discovery/impl/NacosDiscoveryService.java`
- `pollink-server/src/main/java/com/nova/pollink/server/interfaces/controller/PollController.java`
- `pollink-server/src/main/java/com/nova/pollink/server/interfaces/controller/PushController.java`
- `pollink-server/src/main/java/com/nova/pollink/server/application/service/MessageService.java`
- `pollink-server/src/main/java/com/nova/pollink/server/application/service/ConfigService.java`
- `pollink-admin/src/main/java/com/nova/pollink/admin/controller/MessageController.java`
- `pollink-admin/src/main/java/com/nova/pollink/admin/controller/ConfigController.java`
- `pollink-admin/src/main/java/com/nova/pollink/admin/controller/GrayRuleController.java`
- `pollink-admin/src/main/java/com/nova/pollink/admin/controller/NodeController.java`
- `pollink-admin/src/main/java/com/nova/pollink/admin/controller/DashboardController.java`
- `pollink-admin/src/main/resources/application.yml`

### 删除的文件

- `pollink-discovery/src/main/java/com/nova/pollink/discovery/model/ServerNode.java`
- `pollink-server/src/main/java/com/nova/pollink/server/domain/entity/` (目录)
- `pollink-server/src/main/java/com/nova/pollink/server/domain/repository/` (目录)
- `pollink-server/src/main/java/com/nova/pollink/server/infrastructure/repository/` (目录)

---

## Task 1: discovery 模块 — 添加 mybatis 依赖

**Files:**
- Modify: `pollink-discovery/pom.xml`

- [ ] **Step 1: 添加 mybatis-spring-boot-starter 依赖**

将 `spring-boot-starter-jdbc` 替换为 `mybatis-spring-boot-starter`：

```xml
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-jdbc</artifactId>
        </dependency>
```

替换为：

```xml
        <dependency>
            <groupId>org.mybatis.spring.boot</groupId>
            <artifactId>mybatis-spring-boot-starter</artifactId>
            <version>${mybatis-spring-boot.version}</version>
        </dependency>
```

- [ ] **Step 2: Commit**

```bash
git add pollink-discovery/pom.xml
git commit -m "chore(discovery): add mybatis-spring-boot-starter dependency"
```

---

## Task 2: discovery 模块 — 新建 ServerNodeMapper 和迁移 ServerNode

**Files:**
- Create: `pollink-discovery/src/main/java/com/nova/pollink/discovery/dal/mapper/ServerNodeMapper.java`
- Create: `pollink-discovery/src/main/java/com/nova/pollink/discovery/dal/entity/ServerNode.java`
- Modify: `pollink-discovery/src/main/java/com/nova/pollink/discovery/DiscoveryService.java`
- Modify: `pollink-discovery/src/main/java/com/nova/pollink/discovery/impl/MysqlDiscoveryService.java`
- Modify: `pollink-discovery/src/main/java/com/nova/pollink/discovery/impl/NacosDiscoveryService.java`
- Delete: `pollink-discovery/src/main/java/com/nova/pollink/discovery/model/ServerNode.java`

- [ ] **Step 1: 新建 ServerNodeMapper**

```java
package com.nova.pollink.discovery.dal.mapper;

import com.nova.pollink.discovery.dal.entity.ServerNode;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ServerNodeMapper {

    @Insert("""
        INSERT INTO server_nodes (id, ip, status, last_heartbeat, connection_count, create_time)
        VALUES (#{id}, #{ip}, #{status}, #{lastHeartbeat}, #{connectionCount}, #{createTime})
        ON DUPLICATE KEY UPDATE
            status = VALUES(status),
            last_heartbeat = VALUES(last_heartbeat),
            connection_count = VALUES(connection_count)
        """)
    void insertOrUpdate(ServerNode node);

    @Update("UPDATE server_nodes SET last_heartbeat = #{heartbeatTime} WHERE id = #{nodeId}")
    void updateHeartbeat(@Param("nodeId") String nodeId, @Param("heartbeatTime") LocalDateTime heartbeatTime);

    @Update("UPDATE server_nodes SET status = #{status} WHERE id = #{nodeId}")
    void updateStatus(@Param("nodeId") String nodeId, @Param("status") int status);

    @Select("""
        SELECT id, ip, status, last_heartbeat, connection_count, create_time
        FROM server_nodes
        WHERE status = 1 AND last_heartbeat > #{threshold}
        """)
    List<ServerNode> selectActiveNodes(@Param("threshold") LocalDateTime threshold);

    @Update("UPDATE server_nodes SET connection_count = #{count} WHERE id = #{nodeId}")
    void updateConnectionCount(@Param("nodeId") String nodeId, @Param("count") int count);
}
```

- [ ] **Step 2: 新建 ServerNode 实体到 dal/entity**

```java
package com.nova.pollink.discovery.dal.entity;

import java.time.LocalDateTime;

public class ServerNode {

    private String id;
    private String ip;
    private int status;
    private LocalDateTime lastHeartbeat;
    private int connectionCount;
    private LocalDateTime createTime;

    public ServerNode() {}

    public ServerNode(String id, String ip) {
        this.id = id;
        this.ip = ip;
        this.status = 1;
        this.connectionCount = 0;
        this.lastHeartbeat = LocalDateTime.now();
        this.createTime = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public LocalDateTime getLastHeartbeat() { return lastHeartbeat; }
    public void setLastHeartbeat(LocalDateTime lastHeartbeat) { this.lastHeartbeat = lastHeartbeat; }

    public int getConnectionCount() { return connectionCount; }
    public void setConnectionCount(int connectionCount) { this.connectionCount = connectionCount; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
```

- [ ] **Step 3: 修改 DiscoveryService 接口的 import**

`pollink-discovery/src/main/java/com/nova/pollink/discovery/DiscoveryService.java`

替换 import：
```java
import com.nova.pollink.discovery.model.ServerNode;
```
→
```java
import com.nova.pollink.discovery.dal.entity.ServerNode;
```

- [ ] **Step 4: 修改 NacosDiscoveryService 的 import**

`pollink-discovery/src/main/java/com/nova/pollink/discovery/impl/NacosDiscoveryService.java`

替换 import：
```java
import com.nova.pollink.discovery.model.ServerNode;
```
→
```java
import com.nova.pollink.discovery.dal.entity.ServerNode;
```

- [ ] **Step 5: 重写 MysqlDiscoveryService**

`pollink-discovery/src/main/java/com/nova/pollink/discovery/impl/MysqlDiscoveryService.java`

完整替换为：

```java
package com.nova.pollink.discovery.impl;

import com.nova.pollink.discovery.DiscoveryProperties;
import com.nova.pollink.discovery.DiscoveryService;
import com.nova.pollink.discovery.dal.entity.ServerNode;
import com.nova.pollink.discovery.dal.mapper.ServerNodeMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@ConditionalOnProperty(prefix = "nova.pollink.discovery", name = "type", havingValue = "mysql", matchIfMissing = true)
public class MysqlDiscoveryService implements DiscoveryService {

    private final ServerNodeMapper serverNodeMapper;
    private final DiscoveryProperties properties;

    public MysqlDiscoveryService(ServerNodeMapper serverNodeMapper, DiscoveryProperties properties) {
        this.serverNodeMapper = serverNodeMapper;
        this.properties = properties;
    }

    @Override
    public void register(ServerNode node) {
        serverNodeMapper.insertOrUpdate(node);
    }

    @Override
    public void heartbeat(String nodeId) {
        serverNodeMapper.updateHeartbeat(nodeId, LocalDateTime.now());
    }

    @Override
    public void deregister(String nodeId) {
        serverNodeMapper.updateStatus(nodeId, 0);
    }

    @Override
    public List<ServerNode> listActiveNodes() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(properties.getNodeTimeoutSeconds());
        return serverNodeMapper.selectActiveNodes(threshold);
    }

    @Override
    public void updateConnectionCount(String nodeId, int connectionCount) {
        serverNodeMapper.updateConnectionCount(nodeId, connectionCount);
    }
}
```

- [ ] **Step 6: 删除旧 ServerNode**

```bash
rm pollink-discovery/src/main/java/com/nova/pollink/discovery/model/ServerNode.java
```

- [ ] **Step 7: 编译验证**

Run: `cd pollink-discovery && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 8: Commit**

```bash
git add pollink-discovery/
git commit -m "refactor(discovery): migrate JdbcTemplate to MyBatis Mapper in dal package"
```

---

## Task 3: server 模块 — 移动 Message/Config entity 和 mapper 到 dal

**Files:**
- Create: `pollink-server/src/main/java/com/nova/pollink/server/dal/entity/Message.java`
- Create: `pollink-server/src/main/java/com/nova/pollink/server/dal/entity/Config.java`
- Create: `pollink-server/src/main/java/com/nova/pollink/server/dal/mapper/MessageMapper.java`
- Create: `pollink-server/src/main/java/com/nova/pollink/server/dal/mapper/ConfigMapper.java`
- Modify: `pollink-server/src/main/java/com/nova/pollink/server/interfaces/controller/PollController.java`
- Modify: `pollink-server/src/main/java/com/nova/pollink/server/interfaces/controller/PushController.java`
- Modify: `pollink-server/src/main/java/com/nova/pollink/server/application/service/MessageService.java`
- Modify: `pollink-server/src/main/java/com/nova/pollink/server/application/service/ConfigService.java`
- Delete: `pollink-server/src/main/java/com/nova/pollink/server/domain/entity/Message.java`
- Delete: `pollink-server/src/main/java/com/nova/pollink/server/domain/entity/Config.java`
- Delete: `pollink-server/src/main/java/com/nova/pollink/server/infrastructure/repository/MessageMapper.java`
- Delete: `pollink-server/src/main/java/com/nova/pollink/server/infrastructure/repository/ConfigMapper.java`

- [ ] **Step 1: 新建 Message 到 dal/entity（复制，仅改 package）**

`pollink-server/src/main/java/com/nova/pollink/server/dal/entity/Message.java`

```java
package com.nova.pollink.server.dal.entity;

import java.time.LocalDateTime;

public class Message {

    private Long id;
    private LocalDateTime createTime;
    private String topic;
    private String clientFilter;
    private String payload;
    private int status;
    private LocalDateTime expireTime;

    public Message() {}

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expireTime);
    }

    public boolean isPending() {
        return status == 0;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public String getClientFilter() { return clientFilter; }
    public void setClientFilter(String clientFilter) { this.clientFilter = clientFilter; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public LocalDateTime getExpireTime() { return expireTime; }
    public void setExpireTime(LocalDateTime expireTime) { this.expireTime = expireTime; }
}
```

- [ ] **Step 2: 新建 Config 到 dal/entity（复制，仅改 package）**

`pollink-server/src/main/java/com/nova/pollink/server/dal/entity/Config.java`

```java
package com.nova.pollink.server.dal.entity;

import java.time.LocalDateTime;

public class Config {

    private Long id;
    private LocalDateTime createTime;
    private String key;
    private String value;
    private int version;
    private String clientFilter;
    private int status;
    private LocalDateTime updateTime;

    public Config() {}

    public boolean isPublished() {
        return status == 1;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public String getClientFilter() { return clientFilter; }
    public void setClientFilter(String clientFilter) { this.clientFilter = clientFilter; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
```

- [ ] **Step 3: 新建 MessageMapper 到 dal/mapper（复制，改 package + import）**

`pollink-server/src/main/java/com/nova/pollink/server/dal/mapper/MessageMapper.java`

```java
package com.nova.pollink.server.dal.mapper;

import com.nova.pollink.server.dal.entity.Message;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface MessageMapper {

    @Insert("""
        INSERT INTO messages (topic, client_filter, payload, status, expire_time)
        VALUES (#{topic}, #{clientFilter}, #{payload}, #{status}, #{expireTime})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Message message);

    @Select("""
        SELECT id, create_time, topic, client_filter, payload, status, expire_time
        FROM messages
        WHERE topic = #{topic} AND status = 0 AND id > #{lastId}
        ORDER BY id
        LIMIT #{limit}
        """)
    List<Message> selectPendingByTopic(@Param("topic") String topic,
                                       @Param("lastId") Long lastId,
                                       @Param("limit") int limit);

    @Update("UPDATE messages SET status = #{status} WHERE id = #{id}")
    void updateStatus(@Param("id") Long id, @Param("status") int status);

    @Update("UPDATE messages SET status = 2 WHERE status = 0 AND expire_time < NOW()")
    int cleanExpired();
}
```

- [ ] **Step 4: 新建 ConfigMapper 到 dal/mapper（复制，改 package + import）**

`pollink-server/src/main/java/com/nova/pollink/server/dal/mapper/ConfigMapper.java`

```java
package com.nova.pollink.server.dal.mapper;

import com.nova.pollink.server.dal.entity.Config;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ConfigMapper {

    @Insert("""
        INSERT INTO configs (`key`, value, version, client_filter, status)
        VALUES (#{key}, #{value}, #{version}, #{clientFilter}, #{status})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Config config);

    @Select("""
        SELECT id, create_time, `key`, value, version, client_filter, status, update_time
        FROM configs WHERE `key` = #{key}
        """)
    Optional<Config> selectByKey(String key);

    @Select("""
        SELECT id, create_time, `key`, value, version, client_filter, status, update_time
        FROM configs
        WHERE status = 1 AND version > #{version}
        ORDER BY version
        """)
    List<Config> selectPublishedAfterVersion(int version);

    @Update("UPDATE configs SET status = #{status} WHERE id = #{id}")
    void updateStatus(@Param("id") Long id, @Param("status") int status);

    @Update("UPDATE configs SET version = version + 1 WHERE id = #{id}")
    void incrementVersion(Long id);
}
```

- [ ] **Step 5: 修改引用 Message 的文件的 import**

修改 `PollController.java` 的 import：
```java
import com.nova.pollink.server.domain.entity.Message;
```
→
```java
import com.nova.pollink.server.dal.entity.Message;
```

修改 `PushController.java` 的 import：
```java
import com.nova.pollink.server.domain.entity.Message;
```
→
```java
import com.nova.pollink.server.dal.entity.Message;
```

修改 `MessageService.java` 的 import：
```java
import com.nova.pollink.server.domain.entity.Message;
```
→
```java
import com.nova.pollink.server.dal.entity.Message;
```

- [ ] **Step 6: 修改引用 Config 的文件的 import**

修改 `PollController.java` 的 import：
```java
import com.nova.pollink.server.domain.entity.Config;
```
→
```java
import com.nova.pollink.server.dal.entity.Config;
```

修改 `PushController.java` 的 import：
```java
import com.nova.pollink.server.domain.entity.Config;
```
→
```java
import com.nova.pollink.server.dal.entity.Config;
```

修改 `ConfigService.java` 的 import：
```java
import com.nova.pollink.server.domain.entity.Config;
```
→
```java
import com.nova.pollink.server.dal.entity.Config;
```

- [ ] **Step 7: 删除旧 entity 和 mapper**

```bash
rm pollink-server/src/main/java/com/nova/pollink/server/domain/entity/Message.java
rm pollink-server/src/main/java/com/nova/pollink/server/domain/entity/Config.java
rm pollink-server/src/main/java/com/nova/pollink/server/infrastructure/repository/MessageMapper.java
rm pollink-server/src/main/java/com/nova/pollink/server/infrastructure/repository/ConfigMapper.java
```

- [ ] **Step 8: 编译验证**

Run: `cd pollink-server && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 9: Commit**

```bash
git add pollink-server/
git commit -m "refactor(server): move Message/Config entity and mapper to dal package"
```

---

## Task 4: server 模块 — 移动 Repository 接口和实现到 dal

**Files:**
- Create: `pollink-server/src/main/java/com/nova/pollink/server/dal/repository/MessageRepository.java`
- Create: `pollink-server/src/main/java/com/nova/pollink/server/dal/repository/ConfigRepository.java`
- Create: `pollink-server/src/main/java/com/nova/pollink/server/dal/repository/MessageRepositoryImpl.java`
- Create: `pollink-server/src/main/java/com/nova/pollink/server/dal/repository/ConfigRepositoryImpl.java`
- Modify: `pollink-server/src/main/java/com/nova/pollink/server/application/service/MessageService.java`
- Modify: `pollink-server/src/main/java/com/nova/pollink/server/application/service/ConfigService.java`
- Delete: `pollink-server/src/main/java/com/nova/pollink/server/domain/repository/MessageRepository.java`
- Delete: `pollink-server/src/main/java/com/nova/pollink/server/domain/repository/ConfigRepository.java`
- Delete: `pollink-server/src/main/java/com/nova/pollink/server/infrastructure/repository/MessageRepositoryImpl.java`
- Delete: `pollink-server/src/main/java/com/nova/pollink/server/infrastructure/repository/ConfigRepositoryImpl.java`

- [ ] **Step 1: 新建 MessageRepository 到 dal/repository（复制，改 package + import）**

`pollink-server/src/main/java/com/nova/pollink/server/dal/repository/MessageRepository.java`

```java
package com.nova.pollink.server.dal.repository;

import com.nova.pollink.server.dal.entity.Message;
import java.util.List;

public interface MessageRepository {
    void save(Message message);
    List<Message> findPendingByTopic(String topic, Long lastId, int limit);
    void updateStatus(Long id, int status);
    int cleanExpired();
}
```

- [ ] **Step 2: 新建 ConfigRepository 到 dal/repository（复制，改 package + import）**

`pollink-server/src/main/java/com/nova/pollink/server/dal/repository/ConfigRepository.java`

```java
package com.nova.pollink.server.dal.repository;

import com.nova.pollink.server.dal.entity.Config;
import java.util.List;
import java.util.Optional;

public interface ConfigRepository {
    void save(Config config);
    Optional<Config> findByKey(String key);
    List<Config> findPublishedAfterVersion(int version);
    void updateStatus(Long id, int status);
    void incrementVersion(Long id);
}
```

- [ ] **Step 3: 新建 MessageRepositoryImpl 到 dal/repository（复制，改 package + import）**

`pollink-server/src/main/java/com/nova/pollink/server/dal/repository/MessageRepositoryImpl.java`

```java
package com.nova.pollink.server.dal.repository;

import com.nova.pollink.server.dal.entity.Message;
import com.nova.pollink.server.dal.mapper.MessageMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MessageRepositoryImpl implements MessageRepository {

    private final MessageMapper messageMapper;

    public MessageRepositoryImpl(MessageMapper messageMapper) {
        this.messageMapper = messageMapper;
    }

    @Override
    public void save(Message message) {
        messageMapper.insert(message);
    }

    @Override
    public List<Message> findPendingByTopic(String topic, Long lastId, int limit) {
        return messageMapper.selectPendingByTopic(topic, lastId, limit);
    }

    @Override
    public void updateStatus(Long id, int status) {
        messageMapper.updateStatus(id, status);
    }

    @Override
    public int cleanExpired() {
        return messageMapper.cleanExpired();
    }
}
```

- [ ] **Step 4: 新建 ConfigRepositoryImpl 到 dal/repository（复制，改 package + import）**

`pollink-server/src/main/java/com/nova/pollink/server/dal/repository/ConfigRepositoryImpl.java`

```java
package com.nova.pollink.server.dal.repository;

import com.nova.pollink.server.dal.entity.Config;
import com.nova.pollink.server.dal.mapper.ConfigMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ConfigRepositoryImpl implements ConfigRepository {

    private final ConfigMapper configMapper;

    public ConfigRepositoryImpl(ConfigMapper configMapper) {
        this.configMapper = configMapper;
    }

    @Override
    public void save(Config config) {
        configMapper.insert(config);
    }

    @Override
    public Optional<Config> findByKey(String key) {
        return configMapper.selectByKey(key);
    }

    @Override
    public List<Config> findPublishedAfterVersion(int version) {
        return configMapper.selectPublishedAfterVersion(version);
    }

    @Override
    public void updateStatus(Long id, int status) {
        configMapper.updateStatus(id, status);
    }

    @Override
    public void incrementVersion(Long id) {
        configMapper.incrementVersion(id);
    }
}
```

- [ ] **Step 5: 修改 MessageService 的 import**

`pollink-server/src/main/java/com/nova/pollink/server/application/service/MessageService.java`

替换 import：
```java
import com.nova.pollink.server.domain.repository.MessageRepository;
```
→
```java
import com.nova.pollink.server.dal.repository.MessageRepository;
```

- [ ] **Step 6: 修改 ConfigService 的 import**

`pollink-server/src/main/java/com/nova/pollink/server/application/service/ConfigService.java`

替换 import：
```java
import com.nova.pollink.server.domain.repository.ConfigRepository;
```
→
```java
import com.nova.pollink.server.dal.repository.ConfigRepository;
```

- [ ] **Step 7: 删除旧 repository 文件**

```bash
rm pollink-server/src/main/java/com/nova/pollink/server/domain/repository/MessageRepository.java
rm pollink-server/src/main/java/com/nova/pollink/server/domain/repository/ConfigRepository.java
rm pollink-server/src/main/java/com/nova/pollink/server/infrastructure/repository/MessageRepositoryImpl.java
rm pollink-server/src/main/java/com/nova/pollink/server/infrastructure/repository/ConfigRepositoryImpl.java
```

- [ ] **Step 8: 编译验证**

Run: `cd pollink-server && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 9: Commit**

```bash
git add pollink-server/
git commit -m "refactor(server): move Repository interfaces and implementations to dal package"
```

---

## Task 5: admin 模块 — 新建 Entity 类

**Files:**
- Create: `pollink-admin/src/main/java/com/nova/pollink/admin/dal/entity/MessageEntity.java`
- Create: `pollink-admin/src/main/java/com/nova/pollink/admin/dal/entity/ConfigEntity.java`
- Create: `pollink-admin/src/main/java/com/nova/pollink/admin/dal/entity/GrayRuleEntity.java`
- Create: `pollink-admin/src/main/java/com/nova/pollink/admin/dal/entity/ServerNodeEntity.java`

- [ ] **Step 1: 新建 MessageEntity**

`pollink-admin/src/main/java/com/nova/pollink/admin/dal/entity/MessageEntity.java`

```java
package com.nova.pollink.admin.dal.entity;

import java.time.LocalDateTime;

public class MessageEntity {
    private Long id;
    private String topic;
    private String payload;
    private int status;
    private LocalDateTime createTime;
    private LocalDateTime expireTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getExpireTime() { return expireTime; }
    public void setExpireTime(LocalDateTime expireTime) { this.expireTime = expireTime; }
}
```

- [ ] **Step 2: 新建 ConfigEntity**

`pollink-admin/src/main/java/com/nova/pollink/admin/dal/entity/ConfigEntity.java`

```java
package com.nova.pollink.admin.dal.entity;

import java.time.LocalDateTime;

public class ConfigEntity {
    private Long id;
    private String key;
    private String value;
    private int version;
    private int status;
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
```

- [ ] **Step 3: 新建 GrayRuleEntity**

`pollink-admin/src/main/java/com/nova/pollink/admin/dal/entity/GrayRuleEntity.java`

```java
package com.nova.pollink.admin.dal.entity;

import java.time.LocalDateTime;

public class GrayRuleEntity {
    private Long id;
    private String name;
    private int type;
    private Long targetId;
    private String filterJson;
    private int status;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getType() { return type; }
    public void setType(int type) { this.type = type; }

    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }

    public String getFilterJson() { return filterJson; }
    public void setFilterJson(String filterJson) { this.filterJson = filterJson; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
```

- [ ] **Step 4: 新建 ServerNodeEntity**

`pollink-admin/src/main/java/com/nova/pollink/admin/dal/entity/ServerNodeEntity.java`

```java
package com.nova.pollink.admin.dal.entity;

import java.time.LocalDateTime;

public class ServerNodeEntity {
    private String id;
    private String ip;
    private int status;
    private LocalDateTime lastHeartbeat;
    private int connectionCount;
    private LocalDateTime createTime;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public LocalDateTime getLastHeartbeat() { return lastHeartbeat; }
    public void setLastHeartbeat(LocalDateTime lastHeartbeat) { this.lastHeartbeat = lastHeartbeat; }

    public int getConnectionCount() { return connectionCount; }
    public void setConnectionCount(int connectionCount) { this.connectionCount = connectionCount; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
```

- [ ] **Step 5: Commit**

```bash
git add pollink-admin/src/main/java/com/nova/pollink/admin/dal/entity/
git commit -m "feat(admin): add dal entities for messages, configs, gray-rules and server-nodes"
```

---

## Task 6: admin 模块 — 新建 Mapper 接口

**Files:**
- Create: `pollink-admin/src/main/java/com/nova/pollink/admin/dal/mapper/MessageMapper.java`
- Create: `pollink-admin/src/main/java/com/nova/pollink/admin/dal/mapper/ConfigMapper.java`
- Create: `pollink-admin/src/main/java/com/nova/pollink/admin/dal/mapper/GrayRuleMapper.java`
- Create: `pollink-admin/src/main/java/com/nova/pollink/admin/dal/mapper/ServerNodeMapper.java`

- [ ] **Step 1: 新建 MessageMapper**

`pollink-admin/src/main/java/com/nova/pollink/admin/dal/mapper/MessageMapper.java`

```java
package com.nova.pollink.admin.dal.mapper;

import com.nova.pollink.admin.dal.entity.MessageEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface MessageMapper {

    @Select("""
        SELECT id, topic, payload, status, create_time, expire_time
        FROM messages ORDER BY id DESC LIMIT #{limit}
        """)
    List<MessageEntity> selectRecent(@Param("limit") int limit);

    @Insert("""
        INSERT INTO messages (topic, payload, status, expire_time)
        VALUES (#{topic}, #{payload}, 0, DATE_ADD(NOW(), INTERVAL 5 MINUTE))
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(MessageEntity message);
}
```

- [ ] **Step 2: 新建 ConfigMapper**

`pollink-admin/src/main/java/com/nova/pollink/admin/dal/mapper/ConfigMapper.java`

```java
package com.nova.pollink.admin.dal.mapper;

import com.nova.pollink.admin.dal.entity.ConfigEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ConfigMapper {

    @Select("""
        SELECT id, `key`, value, version, status, update_time
        FROM configs ORDER BY id DESC
        """)
    List<ConfigEntity> selectAll();

    @Insert("""
        INSERT INTO configs (`key`, value, version, status)
        VALUES (#{key}, #{value}, 1, 0)
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(ConfigEntity config);

    @Update("UPDATE configs SET status = 1, version = version + 1 WHERE id = #{id}")
    void publish(@Param("id") Long id);
}
```

- [ ] **Step 3: 新建 GrayRuleMapper**

`pollink-admin/src/main/java/com/nova/pollink/admin/dal/mapper/GrayRuleMapper.java`

```java
package com.nova.pollink.admin.dal.mapper;

import com.nova.pollink.admin.dal.entity.GrayRuleEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface GrayRuleMapper {

    @Select("""
        SELECT id, name, type, target_id, filter_json, status, create_time
        FROM gray_rules ORDER BY id DESC
        """)
    List<GrayRuleEntity> selectAll();

    @Insert("""
        INSERT INTO gray_rules (name, type, target_id, filter_json, status)
        VALUES (#{name}, #{type}, #{targetId}, #{filterJson}, 0)
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(GrayRuleEntity rule);

    @Update("UPDATE gray_rules SET status = 1 WHERE id = #{id}")
    void enable(@Param("id") Long id);

    @Update("UPDATE gray_rules SET status = 0 WHERE id = #{id}")
    void disable(@Param("id") Long id);

    @Delete("DELETE FROM gray_rules WHERE id = #{id}")
    void delete(@Param("id") Long id);
}
```

- [ ] **Step 4: 新建 ServerNodeMapper**

`pollink-admin/src/main/java/com/nova/pollink/admin/dal/mapper/ServerNodeMapper.java`

```java
package com.nova.pollink.admin.dal.mapper;

import org.apache.ibatis.annotations.*;

@Mapper
public interface ServerNodeMapper {

    @Update("UPDATE server_nodes SET status = #{status} WHERE id = #{nodeId}")
    int updateStatus(@Param("nodeId") String nodeId, @Param("status") int status);
}
```

- [ ] **Step 5: Commit**

```bash
git add pollink-admin/src/main/java/com/nova/pollink/admin/dal/mapper/
git commit -m "feat(admin): add MyBatis Mappers for messages, configs, gray-rules and server-nodes"
```

---

## Task 7: admin 模块 — 修改 Controller 注入 Mapper

**Files:**
- Modify: `pollink-admin/src/main/java/com/nova/pollink/admin/controller/MessageController.java`
- Modify: `pollink-admin/src/main/java/com/nova/pollink/admin/controller/ConfigController.java`
- Modify: `pollink-admin/src/main/java/com/nova/pollink/admin/controller/GrayRuleController.java`
- Modify: `pollink-admin/src/main/java/com/nova/pollink/admin/controller/NodeController.java`
- Modify: `pollink-admin/src/main/java/com/nova/pollink/admin/controller/DashboardController.java`
- Modify: `pollink-admin/src/main/resources/application.yml`

- [ ] **Step 1: 修改 MessageController**

`pollink-admin/src/main/java/com/nova/pollink/admin/controller/MessageController.java`

完整替换为：

```java
package com.nova.pollink.admin.controller;

import com.nova.pollink.admin.dal.entity.MessageEntity;
import com.nova.pollink.admin.dal.mapper.MessageMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/messages")
public class MessageController {

    private final MessageMapper messageMapper;
    private final RestTemplate restTemplate;
    private final String serverUrl;

    public MessageController(MessageMapper messageMapper,
                             @Value("${nova.pollink.admin.server-url:http://localhost:8080}") String serverUrl) {
        this.messageMapper = messageMapper;
        this.restTemplate = new RestTemplate();
        this.serverUrl = serverUrl.endsWith("/") ? serverUrl.substring(0, serverUrl.length() - 1) : serverUrl;
    }

    @GetMapping
    public List<MessageEntity> listMessages(
            @RequestParam(required = false) String topic,
            @RequestParam(required = false, defaultValue = "20") int limit) {
        return messageMapper.selectRecent(limit);
    }

    @PostMapping("/send")
    public Map<String, String> sendTestMessage(@RequestBody Map<String, String> request) {
        restTemplate.postForObject(serverUrl + "/api/v1/push/message", request, Map.class);
        return Map.of("status", "ok");
    }
}
```

- [ ] **Step 2: 修改 ConfigController**

`pollink-admin/src/main/java/com/nova/pollink/admin/controller/ConfigController.java`

完整替换为：

```java
package com.nova.pollink.admin.controller;

import com.nova.pollink.admin.dal.entity.ConfigEntity;
import com.nova.pollink.admin.dal.mapper.ConfigMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/configs")
public class ConfigController {

    private final ConfigMapper configMapper;
    private final RestTemplate restTemplate;
    private final String serverUrl;

    public ConfigController(ConfigMapper configMapper,
                            @Value("${nova.pollink.admin.server-url:http://localhost:8080}") String serverUrl) {
        this.configMapper = configMapper;
        this.restTemplate = new RestTemplate();
        this.serverUrl = serverUrl.endsWith("/") ? serverUrl.substring(0, serverUrl.length() - 1) : serverUrl;
    }

    @GetMapping
    public List<ConfigEntity> listConfigs() {
        return configMapper.selectAll();
    }

    @PostMapping
    public Map<String, String> createConfig(@RequestBody Map<String, String> request) {
        restTemplate.postForObject(serverUrl + "/api/v1/push/config", request, Map.class);
        return Map.of("status", "ok");
    }

    @PostMapping("/{id}/publish")
    public Map<String, String> publishConfig(@PathVariable Long id) {
        configMapper.publish(id);
        return Map.of("status", "ok");
    }
}
```

- [ ] **Step 3: 修改 GrayRuleController**

`pollink-admin/src/main/java/com/nova/pollink/admin/controller/GrayRuleController.java`

完整替换为：

```java
package com.nova.pollink.admin.controller;

import com.nova.pollink.admin.dal.entity.GrayRuleEntity;
import com.nova.pollink.admin.dal.mapper.GrayRuleMapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/gray-rules")
public class GrayRuleController {

    private final GrayRuleMapper grayRuleMapper;

    public GrayRuleController(GrayRuleMapper grayRuleMapper) {
        this.grayRuleMapper = grayRuleMapper;
    }

    @GetMapping
    public List<GrayRuleEntity> listGrayRules() {
        return grayRuleMapper.selectAll();
    }

    @PostMapping
    public Map<String, String> createGrayRule(@RequestBody Map<String, Object> request) {
        GrayRuleEntity rule = new GrayRuleEntity();
        rule.setName((String) request.get("name"));
        rule.setType((Integer) request.get("type"));
        rule.setTargetId(((Number) request.get("targetId")).longValue());
        rule.setFilterJson((String) request.get("filterJson"));
        grayRuleMapper.insert(rule);
        return Map.of("status", "ok");
    }

    @PostMapping("/{id}/enable")
    public Map<String, String> enableGrayRule(@PathVariable Long id) {
        grayRuleMapper.enable(id);
        return Map.of("status", "ok");
    }

    @PostMapping("/{id}/disable")
    public Map<String, String> disableGrayRule(@PathVariable Long id) {
        grayRuleMapper.disable(id);
        return Map.of("status", "ok");
    }

    @DeleteMapping("/{id}")
    public Map<String, String> deleteGrayRule(@PathVariable Long id) {
        grayRuleMapper.delete(id);
        return Map.of("status", "ok");
    }
}
```

- [ ] **Step 4: 修改 NodeController**

`pollink-admin/src/main/java/com/nova/pollink/admin/controller/NodeController.java`

完整替换为：

```java
package com.nova.pollink.admin.controller;

import com.nova.pollink.admin.dal.mapper.ServerNodeMapper;
import com.nova.pollink.discovery.DiscoveryService;
import com.nova.pollink.discovery.dal.entity.ServerNode;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/nodes")
public class NodeController {

    private final DiscoveryService discoveryService;
    private final ServerNodeMapper serverNodeMapper;

    public NodeController(DiscoveryService discoveryService, ServerNodeMapper serverNodeMapper) {
        this.discoveryService = discoveryService;
        this.serverNodeMapper = serverNodeMapper;
    }

    @GetMapping
    public List<ServerNode> listNodes() {
        return discoveryService.listActiveNodes();
    }

    @PostMapping("/{nodeId}/maintenance")
    public Map<String, String> setMaintenance(@PathVariable String nodeId) {
        int updated = serverNodeMapper.updateStatus(nodeId, 2);
        if (updated > 0) {
            return Map.of("status", "ok", "message", "Node " + nodeId + " set to maintenance");
        }
        return Map.of("status", "error", "message", "Node " + nodeId + " not found");
    }
}
```

- [ ] **Step 5: 修改 DashboardController 的 import**

`pollink-admin/src/main/java/com/nova/pollink/admin/controller/DashboardController.java`

替换 import：
```java
import com.nova.pollink.discovery.model.ServerNode;
```
→
```java
import com.nova.pollink.discovery.dal.entity.ServerNode;
```

- [ ] **Step 6: 配置 Jackson snake_case 序列化**

`pollink-admin/src/main/resources/application.yml`

在现有配置后添加：

```yaml
spring:
  jackson:
    property-naming-strategy: SNAKE_CASE
```

这样前端 JS 中 `msg.create_time`、`rule.target_id` 等访问方式无需修改。

- [ ] **Step 7: 编译验证**

Run: `cd pollink-admin && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 8: Commit**

```bash
git add pollink-admin/
git commit -m "refactor(admin): replace JdbcTemplate with MyBatis Mappers in dal package"
```

---

## Task 8: 清理空目录并全项目编译验证

- [ ] **Step 1: 删除 server 空目录**

```bash
rm -rf pollink-server/src/main/java/com/nova/pollink/server/domain/
rm -rf pollink-server/src/main/java/com/nova/pollink/server/infrastructure/
```

- [ ] **Step 2: 全项目编译验证**

Run: `mvn compile -q`
Expected: BUILD SUCCESS（所有模块编译通过）

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "chore: clean up empty directories after dal migration"
```

---

## Spec Coverage Self-Review

| 设计文档章节 | 对应任务 | 状态 |
|-------------|---------|------|
| discovery 模块：添加 mybatis + ServerNodeMapper | Task 1, Task 2 | 已覆盖 |
| server 模块：移动 entity + mapper 到 dal | Task 3 | 已覆盖 |
| server 模块：移动 repository 到 dal | Task 4 | 已覆盖 |
| admin 模块：新建 entity | Task 5 | 已覆盖 |
| admin 模块：新建 mapper | Task 6 | 已覆盖 |
| admin 模块：修改 controller + Jackson 配置 | Task 7 | 已覆盖 |
| 清理旧文件 + 全项目编译 | Task 8 | 已覆盖 |

**无未覆盖需求。**

**Placeholder scan:** 无 TBD/TODO/implement later。

**Type consistency:**
- `ServerNode` 在 discovery 的 dal/entity 中使用，admin 的 NodeController 和 DashboardController 引用同一类型。
- `GrayRuleEntity.targetId` 为 Long，与 Controller 中的 `((Number) request.get("targetId")).longValue()` 一致。
- 所有 Mapper 方法签名与 Controller 调用一致。
