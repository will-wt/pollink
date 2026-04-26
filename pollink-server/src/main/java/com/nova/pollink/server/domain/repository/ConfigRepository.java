package com.nova.pollink.server.domain.repository;

import com.nova.pollink.server.domain.entity.Config;
import java.util.List;
import java.util.Optional;

/**
 * 配置仓储接口。
 * 定义领域层对配置数据的访问契约，由基础设施层实现。
 */
public interface ConfigRepository {

    /**
     * 保存配置。
     */
    void save(Config config);

    /**
     * 按 key 查询配置。
     */
    Optional<Config> findByKey(String key);

    /**
     * 查询版本号大于指定值的已发布配置。
     */
    List<Config> findPublishedAfterVersion(int version);

    /**
     * 更新配置状态。
     */
    void updateStatus(Long id, int status);

    /**
     * 递增版本号。
     */
    void incrementVersion(Long id);
}
