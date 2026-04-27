# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Pollink is a lightweight long-polling framework for backend, supporting cluster deployment. It provides persistent HTTP connections, request hanging, async message push, and configuration distribution. The framework consists of 5 Maven modules.

## Build & Development

```bash
# Compile all modules
mvn compile -q

# Install to local repo (required before dependent modules can compile)
mvn install -DskipTests

# Compile a specific module with its dependencies
mvn compile -pl pollink-server -am -q

# There are currently no unit tests in this project.
```

**Key versions:** Java 17, Spring Boot 3.2.0, gRPC 1.59.0, MyBatis 3.0.3, Protobuf 3.25.0.

**Protobuf:** The server module uses `protobuf-maven-plugin` to generate Java classes from `pollink-server/src/main/proto/node.proto`. Generated sources go to `target/generated-sources/protobuf/`.

## Module Structure

| Module | Role | Port | Notes |
|--------|------|------|-------|
| `pollink-discovery` | Service discovery SPI | — | `DiscoveryService` interface with MySQL (default) and Nacos (stub) implementations |
| `pollink-server` | Core long-polling server | 8080 (HTTP), 9101 (gRPC) | Holds client connections, pushes messages/configs, broadcasts to peers |
| `pollink-client` | Pure Java HTTP client | — | No Spring dependency; uses `java.net.http.HttpClient` |
| `pollink-admin` | Management web UI + REST API | 8090 | Static HTML/CSS/JS frontend; REST endpoints for messages, configs, nodes, gray rules |
| `pollink-example` | Demo launcher | — | Depends on server + client |

## Architecture

### Long Polling Core

- `PollController` (`server/interfaces/controller/`) exposes `/api/v1/poll/messages` and `/api/v1/poll/configs`. Uses `DeferredResult` to hold HTTP connections open for up to 30 seconds.
- Pending polls are stored in typed `ConcurrentHashMap`s (`pendingMessagePolls`, `pendingConfigPolls`), keyed by `topic:clientId:seq`.
- `PollController.wakeupPendingPolls(topic)` is called when new data arrives, returning empty lists so clients re-poll immediately.

### Cluster Communication (gRPC)

- Nodes discover each other via MySQL `server_nodes` table with heartbeats.
- `NodeGrpcClient` (`server/interfaces/grpc/`) maintains persistent bidirectional gRPC streams to all peer nodes.
- `NodeServiceImpl` (`server/infrastructure/grpc/`) receives data notifications from peers and wakes local pending polls.
- `PushController` writes data locally then calls `NodeGrpcClient.notifyPeers()` to broadcast.
- gRPC uses plaintext by default; TLS can be enabled via `nova.pollink.server.grpc-use-plaintext=false`.

### Service Discovery

- `DiscoveryService` SPI lives in `pollink-discovery`.
- `MysqlDiscoveryService` (default) operates on the `server_nodes` table: register on startup, heartbeat every 5s, deregister on shutdown.
- Node status: 0=offline, 1=online, 2=maintenance.

### Graceful Shutdown

- `GracefulShutdownConfig` implements `SmartLifecycle` with phase `Integer.MAX_VALUE - 10`.
- Sequence: (1) reject new polls (`acceptingRequests=false`), (2) wake all pending polls, (3) stop gRPC server, (4) wait 2s, (5) deregister from discovery.

### Data Access (DAL)

- All SQL is in MyBatis `@Mapper` interfaces under `<module>/dal/mapper/`.
- Entities are under `<module>/dal/entity/`.
- `map-underscore-to-camel-case: true` is configured globally.
- Admin module uses `spring.jackson.property-naming-strategy: SNAKE_CASE` so frontend JS `msg.create_time` etc. still works.

### Client Behavior

- `LongPollingClient` polls both messages (by topic) and configs (by version) in separate threads.
- Exponential backoff on errors (1s → 2s → 4s → 8s, capped at 30s).
- 503 responses trigger 5-second backoff.
- Backpressure via `hasMore` flag: if server indicates more data exists, client skips the sleep interval.

### Admin Write Flow

- Admin query operations read directly from DB via Mappers.
- Admin write operations (send message, create config) call the **server's** `/api/v1/push/*` REST endpoints via `RestTemplate` — this ensures gRPC cluster broadcast and client wakeup are triggered. Never write directly to DB from admin for mutations.

## Database Schema

MySQL database `nova_long_polling` with 4 tables:
- `messages` — topic-based push messages
- `configs` — key-value configs with versioning
- `server_nodes` — node registry with heartbeats
- `gray_rules` — gray release rules for messages/configs

See `sql/init.sql` for full schema.

## Key Configuration

**pollink-server** (`pollink-server/src/main/resources/application.yml`):
- `nova.pollink.server.grpc-port: 9101` — gRPC inter-node port
- `nova.pollink.server.poll-timeout-seconds: 30` — long-polling hold timeout
- `nova.pollink.server.node-ip:` — explicit node IP (auto-detected if empty)
- `nova.pollink.discovery.*` — heartbeat interval (5s), node timeout (15s)

**pollink-admin** (`pollink-admin/src/main/resources/application.yml`):
- `nova.pollink.admin.server-url` — server push API base URL

## Important Conventions

- Do not use `JdbcTemplate` for new code; use MyBatis `@Mapper` interfaces in the `dal` package.
- Server module's `domain/` and `infrastructure/` directories have been migrated to `dal/` — do not recreate them.
- Protobuf enum values must have unique names within the same `java_outer_classname`. The `DataType` enum uses `UNKNOWN_DATA = 0` (not `UNKNOWN`) to avoid collision with `MessageType.UNKNOWN`.
- The `server/` directory at project root is stale/legacy — all active code is under `pollink-server/`.
