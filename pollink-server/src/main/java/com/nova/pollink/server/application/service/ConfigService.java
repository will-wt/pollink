package com.nova.pollink.server.application.service;

import com.nova.pollink.server.dal.entity.Config;
import com.nova.pollink.server.dal.repository.ConfigRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 配置应用服务。
 * 编排配置的发布、查询和版本管理流程。
 * @author wentao
 */
@Service
public class ConfigService {

    private final ConfigRepository configRepository;

    public ConfigService(ConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    /**
     * 创建新配置（草稿状态）。
     */
    public Config createConfig(String key, String value) {
        Config config = new Config();
        config.setKey(key);
        config.setValue(value);
        config.setVersion(1);
        config.setStatus(0);
        LocalDateTime now = LocalDateTime.now();
        config.setCreateTime(now);
        config.setUpdateTime(now);
        configRepository.save(config);
        return config;
    }

    /**
     * 发布配置（将状态改为已发布，版本号 +1）。
     */
    public void publishConfig(Long id) {
        configRepository.incrementVersion(id);
        configRepository.updateStatus(id, 1);
    }

    /**
     * 按版本号查询已发布的配置（用于客户端增量同步）。
     */
    public List<Config> pollConfigs(int lastVersion) {
        return configRepository.findPublishedAfterVersion(lastVersion);
    }

    /**
     * 按 key 查询配置。
     */
    public Optional<Config> getConfig(String key) {
        return configRepository.findByKey(key);
    }
}
