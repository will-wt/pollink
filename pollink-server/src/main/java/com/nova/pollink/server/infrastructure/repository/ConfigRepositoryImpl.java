package com.nova.pollink.server.infrastructure.repository;

import com.nova.pollink.server.dal.entity.Config;
import com.nova.pollink.server.dal.mapper.ConfigMapper;
import com.nova.pollink.server.domain.repository.ConfigRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ConfigRepository 的 MyBatis 实现。
 */
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
