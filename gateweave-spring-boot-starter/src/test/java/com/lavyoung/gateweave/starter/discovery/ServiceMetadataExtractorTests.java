package com.lavyoung.gateweave.starter.discovery;

import com.lavyoung.gateweave.starter.annotation.GateWeaveOperation;
import com.lavyoung.gateweave.starter.annotation.GateWeaveService;
import com.lavyoung.gateweave.starter.metadata.ServiceMetadata;
import org.junit.jupiter.api.Test;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class ServiceMetadataExtractorTests {

    private final ServiceMetadataExtractor extractor =
            new ServiceMetadataExtractor(new DefaultParameterNameDiscoverer());

    @Test
    void shouldExtractAnnotatedServiceInterface() {
        // When
        List<ServiceMetadata> services = this.extractor.extract(CustomerServiceImpl.class, "customer-application");

        // Then
        assertThat(services).singleElement().satisfies(service -> {
            assertThat(service.serviceId()).isEqualTo("customer-service");
            assertThat(service.applicationName()).isEqualTo("customer-application");
            assertThat(service.protocol()).isEqualTo("dubbo");
            assertThat(service.namespace()).isEqualTo("sales");
            assertThat(service.version()).isEqualTo("1.0.0");
            assertThat(service.operations()).extracting(operation -> operation.operationId())
                    .containsExactly("count-customers", "find-customer");
            assertThat(service.operations().get(1).interfaceName()).isEqualTo(CustomerService.class.getName());
            assertThat(service.operations().get(1).operationName()).isEqualTo("findCustomer");
            assertThat(service.operations().get(1).parameters()).singleElement().satisfies(parameter -> {
                assertThat(parameter.position()).isZero();
                assertThat(parameter.name()).isEqualTo("customerId");
                assertThat(parameter.typeName()).isEqualTo(String.class.getName());
            });
        });
    }

    @Test
    void shouldReturnEmptyListForUnannotatedBean() {
        // When
        List<ServiceMetadata> services = this.extractor.extract(UnannotatedService.class, "customer-application");

        // Then
        assertThat(services).isEmpty();
    }

    @Test
    void shouldRejectBlankApplicationName() {
        assertThatThrownBy(() -> this.extractor.extract(CustomerServiceImpl.class, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("applicationName must not be blank");
    }

    @Test
    void shouldRejectDuplicatedServiceIds() {
        assertThatThrownBy(() -> this.extractor.extract(DuplicatedServiceImpl.class, "customer-application"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("serviceIds must not be duplicated");
    }

    @Test
    void shouldRejectMethodWithoutDiscoverableParameterNames() {
        // Given
        ParameterNameDiscoverer parameterNameDiscoverer = mock(ParameterNameDiscoverer.class);
        ServiceMetadataExtractor metadataExtractor = new ServiceMetadataExtractor(parameterNameDiscoverer);

        // When & Then
        assertThatThrownBy(() -> metadataExtractor.extract(CustomerServiceImpl.class, "customer-application"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Parameter names are not available for");
    }

    @GateWeaveService(serviceId = "customer-service", protocol = "dubbo", namespace = "sales", version = "1.0.0")
    private interface CustomerService {

        @GateWeaveOperation(operationId = "find-customer")
        String findCustomer(String customerId);

        @GateWeaveOperation(operationId = "count-customers")
        int countCustomers();
    }

    private static final class CustomerServiceImpl implements CustomerService {

        @Override
        public String findCustomer(String customerId) {
            return customerId;
        }

        @Override
        public int countCustomers() {
            return 0;
        }
    }

    private static final class UnannotatedService {
    }

    @GateWeaveService(serviceId = "duplicated-service")
    private interface FirstDuplicatedService {

        @GateWeaveOperation(operationId = "first-operation")
        void firstOperation();
    }

    @GateWeaveService(serviceId = "duplicated-service")
    private interface SecondDuplicatedService {

        @GateWeaveOperation(operationId = "second-operation")
        void secondOperation();
    }

    private static final class DuplicatedServiceImpl implements FirstDuplicatedService, SecondDuplicatedService {

        @Override
        public void firstOperation() {
        }

        @Override
        public void secondOperation() {
        }
    }
}
