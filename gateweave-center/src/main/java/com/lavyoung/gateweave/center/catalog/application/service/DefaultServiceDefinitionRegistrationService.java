package com.lavyoung.gateweave.center.catalog.application.service;

import com.lavyoung.gateweave.center.catalog.application.port.in.RegisterServiceDefinitionUseCase;
import com.lavyoung.gateweave.center.catalog.domain.ServiceDefinition;
import com.lavyoung.gateweave.center.catalog.domain.repository.ServiceCatalogRepository;
import com.lavyoung.gateweave.contract.catalog.CatalogRegistrationOutcome;

import java.util.Objects;

/**
 * 实现完整服务定义的幂等注册用例。
 *
 * <p>应用服务负责连接入站用例与服务目录持久化端口，不感知 HTTP、JDBC 或数据库异常。
 * 领域模型负责定义完整性校验，Repository 负责事务、并发幂等和冲突识别。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/01
 */
public class DefaultServiceDefinitionRegistrationService implements RegisterServiceDefinitionUseCase {

    private final ServiceCatalogRepository serviceCatalogRepository;

    /**
     * 创建服务定义注册应用服务。
     *
     * @param serviceCatalogRepository 服务目录持久化端口
     * @throws NullPointerException serviceCatalogRepository 为 null 时抛出
     */
    public DefaultServiceDefinitionRegistrationService(ServiceCatalogRepository serviceCatalogRepository) {
        this.serviceCatalogRepository = Objects.requireNonNull(
                serviceCatalogRepository, "serviceCatalogRepository must not be null");
    }

    @Override
    public CatalogRegistrationOutcome register(ServiceDefinition serviceDefinition) {
        Objects.requireNonNull(serviceDefinition, "serviceDefinition must not be null");
        return this.serviceCatalogRepository.register(serviceDefinition);
    }
}
