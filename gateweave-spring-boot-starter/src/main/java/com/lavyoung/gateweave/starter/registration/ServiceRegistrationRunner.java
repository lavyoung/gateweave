package com.lavyoung.gateweave.starter.registration;

import com.lavyoung.gateweave.contract.catalog.CatalogRegistrationOutcome;
import com.lavyoung.gateweave.starter.metadata.ServiceMetadata;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import java.util.Objects;

/**
 * 在 Spring 应用完成单例初始化后注册全部 GateWeave 服务定义。
 *
 * <p>元数据采集发生在 Bean 后置处理阶段，本运行器随后读取稳定快照并顺序提交。
 * 注册失败会直接向 Spring Boot 传播，使应用不会在服务目录缺失时进入 Ready 状态。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/13
 */
public final class ServiceRegistrationRunner implements ApplicationRunner {

    private static final Log LOGGER = LogFactory.getLog(ServiceRegistrationRunner.class);

    private final ServiceMetadataRegistry metadataRegistry;
    private final ServiceRegistrationClient registrationClient;

    /**
     * 创建服务启动注册运行器。
     *
     * @param metadataRegistry   当前应用的服务元数据注册表
     * @param registrationClient 服务注册出站客户端
     * @throws NullPointerException 任一依赖为 null 时抛出
     */
    public ServiceRegistrationRunner(ServiceMetadataRegistry metadataRegistry,
            ServiceRegistrationClient registrationClient) {
        this.metadataRegistry = Objects.requireNonNull(metadataRegistry, "metadataRegistry must not be null");
        this.registrationClient = Objects.requireNonNull(registrationClient, "registrationClient must not be null");
    }

    @Override
    public void run(ApplicationArguments args) {
        for (ServiceMetadata metadata : this.metadataRegistry.snapshot()) {
            CatalogRegistrationOutcome outcome = this.registrationClient.register(metadata);
            LOGGER.info("Registered GateWeave service metadata: serviceId=" + metadata.serviceId()
                    + ", outcome=" + outcome);
        }
    }
}
