package com.lavyoung.gateweave.starter.configuration;

import com.lavyoung.gateweave.starter.discovery.ServiceMetadataBeanPostProcessor;
import com.lavyoung.gateweave.starter.discovery.ServiceMetadataExtractor;
import com.lavyoung.gateweave.starter.metadata.ServiceMetadata;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Role;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.env.Environment;

import java.util.function.Consumer;

/**
 * 自动装配 GateWeave 服务元数据采集组件。
 *
 * <p>该配置复用 Spring Boot 标准的 {@code spring.application.name} 作为应用身份，
 * 并在 Bean 初始化完成后提取带有 GateWeave 注解的服务元数据。本章只记录采集结果，
 * 不负责向控制面发送注册请求。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/11
 */
@AutoConfiguration
public class GateWeaveServiceMetadataAutoConfiguration {

    /**
     * 默认服务元数据消费者的 Bean 名称。
     */
    public static final String SERVICE_METADATA_CONSUMER_BEAN_NAME = "gateWeaveServiceMetadataConsumer";

    private static final Log LOGGER = LogFactory.getLog(GateWeaveServiceMetadataAutoConfiguration.class);

    /**
     * 创建默认服务元数据消费者。
     *
     * <p>后续引入控制面注册能力时，可以声明同名 Bean 替换当前日志消费者。</p>
     *
     * @return 服务元数据消费者
     */
    @Bean(name = SERVICE_METADATA_CONSUMER_BEAN_NAME)
    @ConditionalOnMissingBean(name = SERVICE_METADATA_CONSUMER_BEAN_NAME)
    public static Consumer<ServiceMetadata> gateWeaveServiceMetadataConsumer() {
        return metadata -> LOGGER.info("Discovered GateWeave service metadata: applicationName="
                + metadata.applicationName() + ", serviceId=" + metadata.serviceId()
                + ", operationCount=" + metadata.operations().size());
    }

    /**
     * 创建服务元数据 Bean 后置处理器。
     *
     * <p>消费者通过 ObjectProvider 延迟获取，避免应用 Bean 在注册 BeanPostProcessor
     * 期间被提前实例化。元数据提取器是处理器内部基础设施，不作为业务扩展点暴露。</p>
     *
     * @param environment              Spring 应用环境
     * @param metadataConsumerProvider 服务元数据消费者提供器
     * @return 服务元数据 Bean 后置处理器
     */
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @ConditionalOnMissingBean
    public static ServiceMetadataBeanPostProcessor serviceMetadataBeanPostProcessor(
            Environment environment,
            @Qualifier(SERVICE_METADATA_CONSUMER_BEAN_NAME)
            ObjectProvider<Consumer<ServiceMetadata>> metadataConsumerProvider) {
        String applicationName = environment.getRequiredProperty("spring.application.name");
        // 实例化元数据提取器
        ServiceMetadataExtractor metadataExtractor =
                new ServiceMetadataExtractor(new DefaultParameterNameDiscoverer());
        // 延迟创建服务元数据消费者
        Consumer<ServiceMetadata> lazyMetadataConsumer =
                metadata -> metadataConsumerProvider.getObject().accept(metadata);
        return new ServiceMetadataBeanPostProcessor(metadataExtractor, applicationName, lazyMetadataConsumer);
    }
}
