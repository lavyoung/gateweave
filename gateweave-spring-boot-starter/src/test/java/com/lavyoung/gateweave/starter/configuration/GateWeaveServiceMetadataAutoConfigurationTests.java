package com.lavyoung.gateweave.starter.configuration;

import com.lavyoung.gateweave.starter.annotation.GateWeaveOperation;
import com.lavyoung.gateweave.starter.annotation.GateWeaveService;
import com.lavyoung.gateweave.starter.discovery.ServiceMetadataBeanPostProcessor;
import com.lavyoung.gateweave.starter.metadata.ServiceMetadata;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

class GateWeaveServiceMetadataAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(GateWeaveServiceMetadataAutoConfiguration.class))
            .withPropertyValues("spring.application.name=order-application");

    @Test
    void shouldCreateDefaultMetadataComponents() {
        this.contextRunner.run(context -> {
            assertThat(context).hasSingleBean(ServiceMetadataBeanPostProcessor.class);
            assertThat(context).hasBean(GateWeaveServiceMetadataAutoConfiguration.SERVICE_METADATA_CONSUMER_BEAN_NAME);
        });
    }

    @Test
    void shouldPublishAnnotatedServiceToCustomConsumer() {
        this.contextRunner
                .withBean(GateWeaveServiceMetadataAutoConfiguration.SERVICE_METADATA_CONSUMER_BEAN_NAME,
                        MetadataCollector.class, MetadataCollector::new)
                .withBean("orderService", OrderServiceImpl.class, OrderServiceImpl::new)
                .run(context -> {
                    MetadataCollector collector = context.getBean(
                            GateWeaveServiceMetadataAutoConfiguration.SERVICE_METADATA_CONSUMER_BEAN_NAME,
                            MetadataCollector.class);
                    assertThat(collector.metadata()).singleElement().satisfies(metadata -> {
                        assertThat(metadata.applicationName()).isEqualTo("order-application");
                        assertThat(metadata.serviceId()).isEqualTo("order-service");
                        assertThat(metadata.operations()).singleElement()
                                .satisfies(operation -> assertThat(operation.operationId()).isEqualTo("find-order"));
                    });
                });
    }

    @Test
    void shouldFailWhenApplicationNameIsMissing() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(GateWeaveServiceMetadataAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseMessage(
                            "Required key 'spring.application.name' not found");
                });
    }

    private static final class MetadataCollector implements Consumer<ServiceMetadata> {

        private final List<ServiceMetadata> metadata = new ArrayList<>();

        @Override
        public void accept(ServiceMetadata serviceMetadata) {
            this.metadata.add(serviceMetadata);
        }

        private List<ServiceMetadata> metadata() {
            return List.copyOf(this.metadata);
        }
    }

    @GateWeaveService(serviceId = "order-service")
    private interface OrderService {

        @GateWeaveOperation(operationId = "find-order")
        String findOrder(String orderId);
    }

    private static final class OrderServiceImpl implements OrderService {

        @Override
        public String findOrder(String orderId) {
            return orderId;
        }
    }
}
