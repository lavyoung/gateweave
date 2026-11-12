package com.lavyoung.gateweave.starter.discovery;

import com.lavyoung.gateweave.starter.metadata.ServiceMetadata;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.util.ClassUtils;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * 在 Spring Bean 初始化完成后采集 GateWeave 服务元数据。
 *
 * <p>该处理器只负责衔接 Spring Bean 生命周期与服务元数据提取器，
 * 不负责网络注册、重试或控制面通信。提取结果由调用方提供的消费者处理。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/11
 */
public final class ServiceMetadataBeanPostProcessor implements BeanPostProcessor {

    private final ServiceMetadataExtractor metadataExtractor;
    private final String applicationName;
    private final Consumer<ServiceMetadata> metadataConsumer;


    /**
     * 创建服务元数据 Bean 后置处理器。
     *
     * @param metadataExtractor 服务元数据提取器
     * @param applicationName   服务所属应用名称
     * @param metadataConsumer  服务元数据消费者
     * @throws NullPointerException     任一参数为 null 时抛出
     * @throws IllegalArgumentException applicationName 为空白时抛出
     */
    public ServiceMetadataBeanPostProcessor(ServiceMetadataExtractor metadataExtractor, String applicationName,
            Consumer<ServiceMetadata> metadataConsumer) {
        this.metadataExtractor = Objects.requireNonNull(metadataExtractor, "metadataExtractor must not be null");
        this.applicationName = Objects.requireNonNull(applicationName, "applicationName must not be null").strip();
        this.metadataConsumer = Objects.requireNonNull(metadataConsumer, "metadataConsumer must not be null");
        if (this.applicationName.isEmpty()) {
            throw new IllegalArgumentException("applicationName must not be blank");
        }
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> beanType = ClassUtils.getUserClass(bean);
        this.metadataExtractor.extract(beanType, this.applicationName).forEach(this.metadataConsumer);
        return bean;
    }
}
