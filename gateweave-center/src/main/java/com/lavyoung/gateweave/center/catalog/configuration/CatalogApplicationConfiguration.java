package com.lavyoung.gateweave.center.catalog.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.lavyoung.gateweave.center.catalog.application.RegisterServiceDefinitionUseCase;
import com.lavyoung.gateweave.center.catalog.application.DefaultServiceDefinitionRegistrationService;
import com.lavyoung.gateweave.center.catalog.domain.ServiceCatalogRepository;

/**
 * 装配服务目录限界上下文的应用服务。
 *
 * <p>应用服务保持框架无关，Spring 只在配置边界将注册用例与持久化端口组装起来。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/01
 */
@Configuration(proxyBeanMethods = false)
public class CatalogApplicationConfiguration {

    /**
     * 创建服务定义注册用例。
     *
     * @param serviceCatalogRepository 服务目录持久化端口
     * @return 服务定义注册用例
     */
    @Bean
    public RegisterServiceDefinitionUseCase registerServiceDefinitionUseCase(
            ServiceCatalogRepository serviceCatalogRepository) {
        return new DefaultServiceDefinitionRegistrationService(serviceCatalogRepository);
    }
}
