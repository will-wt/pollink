-- 数据库：nova_long_polling
CREATE DATABASE IF NOT EXISTS nova_long_polling DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE nova_long_polling;

-- 消息表
CREATE TABLE IF NOT EXISTS messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    topic VARCHAR(64) NOT NULL COMMENT '业务通道/主题',
    client_filter VARCHAR(256) COMMENT '目标客户端过滤条件（JSON）',
    payload TEXT NOT NULL COMMENT '消息内容',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0=待推送, 1=已推送, 2=已超时',
    expire_time DATETIME NOT NULL COMMENT '过期时间',
    INDEX idx_topic_status_create_time (topic, status, create_time),
    INDEX idx_expire_time (expire_time),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息表';

-- 配置表
CREATE TABLE IF NOT EXISTS configs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `key` VARCHAR(64) NOT NULL COMMENT '配置键',
    value TEXT NOT NULL COMMENT '配置值',
    version INT NOT NULL DEFAULT 1 COMMENT '版本号',
    client_filter VARCHAR(256) COMMENT '灰度目标过滤条件',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0=草稿, 1=已发布, 2=已回滚',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_key (`key`),
    INDEX idx_status_update_time (status, update_time),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='配置表';

-- 节点注册表
CREATE TABLE IF NOT EXISTS server_nodes (
    id VARCHAR(64) PRIMARY KEY COMMENT '节点标识（ip）',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip VARCHAR(32) NOT NULL COMMENT '节点 IP',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '0=离线, 1=在线, 2=维护中',
    last_heartbeat DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上次心跳时间',
    connection_count INT NOT NULL DEFAULT 0 COMMENT '当前连接数',
    UNIQUE KEY uk_ip (ip),
    INDEX idx_status_last_heartbeat (status, last_heartbeat),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='节点注册表';

-- 灰度规则表
CREATE TABLE IF NOT EXISTS gray_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    name VARCHAR(64) NOT NULL COMMENT '规则名',
    type TINYINT NOT NULL COMMENT '1=消息, 2=配置',
    target_id BIGINT NOT NULL COMMENT '关联的消息/配置 ID',
    filter_json VARCHAR(512) NOT NULL COMMENT '灰度条件（百分比、标签等）',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0=未启用, 1=已启用',
    INDEX idx_type_target_id (type, target_id),
    INDEX idx_status_create_time (status, create_time),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='灰度规则表';
