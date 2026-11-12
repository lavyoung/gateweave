package com.lavyoung.gateweave.starter.discovery;

import com.lavyoung.gateweave.starter.annotation.GateWeaveOperation;
import com.lavyoung.gateweave.starter.annotation.GateWeaveService;
import com.lavyoung.gateweave.starter.metadata.ServiceMetadata;
import org.junit.jupiter.api.Test;
import org.springframework.core.DefaultParameterNameDiscoverer;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServiceMetadataBeanPostProcessorTests {

    private final ServiceMetadataExtractor metadataExtractor =
            new ServiceMetadataExtractor(new DefaultParameterNameDiscoverer());

    @Test
    void shouldPublishMetadataAndReturnOriginalBean() {
        // Given
        List<ServiceMetadata> collectedMetadata = new ArrayList<>();
        ServiceMetadataBeanPostProcessor processor = new ServiceMetadataBeanPostProcessor(this.metadataExtractor,
                " order-application ", collectedMetadata::add);
        OrderServiceImpl bean = new OrderServiceImpl();

        // When
        Object processedBean = processor.postProcessAfterInitialization(bean, "orderService");

        // Then
        assertThat(processedBean).isSameAs(bean);
        assertThat(collectedMetadata).singleElement().satisfies(metadata -> {
            assertThat(metadata.serviceId()).isEqualTo("order-service");
            assertThat(metadata.applicationName()).isEqualTo("order-application");
            assertThat(metadata.operations()).singleElement().satisfies(operation -> {
                assertThat(operation.operationId()).isEqualTo("find-order");
                assertThat(operation.operationName()).isEqualTo("findOrder");
            });
        });
    }

    @Test
    void shouldIgnoreUnannotatedBeanAndReturnOriginalBean() {
        // Given
        List<ServiceMetadata> collectedMetadata = new ArrayList<>();
        ServiceMetadataBeanPostProcessor processor = new ServiceMetadataBeanPostProcessor(this.metadataExtractor,
                "order-application", collectedMetadata::add);
        UnannotatedBean bean = new UnannotatedBean();

        // When
        Object processedBean = processor.postProcessAfterInitialization(bean, "unannotatedBean");

        // Then
        assertThat(processedBean).isSameAs(bean);
        assertThat(collectedMetadata).isEmpty();
    }

    @Test
    void shouldRejectNullConstructorArguments() {
        assertThatNullPointerException()
                .isThrownBy(() -> new ServiceMetadataBeanPostProcessor(null, "order-application", metadata -> {
                })).withMessage("metadataExtractor must not be null");
        assertThatNullPointerException()
                .isThrownBy(() -> new ServiceMetadataBeanPostProcessor(this.metadataExtractor, null, metadata -> {
                })).withMessage("applicationName must not be null");
        assertThatNullPointerException()
                .isThrownBy(() -> new ServiceMetadataBeanPostProcessor(
                        this.metadataExtractor, "order-application", null))
                .withMessage("metadataConsumer must not be null");
    }

    @Test
    void shouldRejectBlankApplicationName() {
        assertThatThrownBy(() -> new ServiceMetadataBeanPostProcessor(this.metadataExtractor, " ", metadata -> {
        })).isInstanceOf(IllegalArgumentException.class).hasMessage("applicationName must not be blank");
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

    private static final class UnannotatedBean {
    }
}
