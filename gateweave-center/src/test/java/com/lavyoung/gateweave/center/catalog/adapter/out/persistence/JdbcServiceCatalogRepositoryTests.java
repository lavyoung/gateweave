package com.lavyoung.gateweave.center.catalog.adapter.out.persistence;

import com.lavyoung.gateweave.center.GateWeaveCenterApplication;
import com.lavyoung.gateweave.center.catalog.domain.ServiceCatalogStatus;
import com.lavyoung.gateweave.center.catalog.domain.ServiceDefinition;
import com.lavyoung.gateweave.center.catalog.domain.ServiceOperation;
import com.lavyoung.gateweave.center.catalog.domain.ServiceOperationParameter;
import com.lavyoung.gateweave.center.catalog.domain.repository.ServiceCatalogRepository;
import com.lavyoung.gateweave.center.shared.exception.BusinessException;
import com.lavyoung.gateweave.contract.catalog.CatalogRegistrationOutcome;
import com.lavyoung.gateweave.contract.error.CatalogResponseCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(
        classes = GateWeaveCenterApplication.class,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:service-catalog-repository;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "spring.datasource.username=sa",
                "spring.datasource.password="
        })
class JdbcServiceCatalogRepositoryTests {

    private final ServiceCatalogRepository repository;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    JdbcServiceCatalogRepositoryTests(ServiceCatalogRepository repository, JdbcTemplate jdbcTemplate) {
        this.repository = repository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @BeforeEach
    void deleteServiceCatalog() {
        this.jdbcTemplate.update("DELETE FROM gw_service_operation_parameter");
        this.jdbcTemplate.update("DELETE FROM gw_service_operation");
        this.jdbcTemplate.update("DELETE FROM gw_service");
    }

    @Test
    void shouldRegisterCompleteServiceDefinitionAtomically() {
        // Given
        ServiceDefinition definition = serviceDefinition("activity-service", "activity-application", List.of(
                operation("activity-query", "activity-service", "query", "name", "java.lang.String"),
                operation("activity-create", "activity-service", "create", "request", "cn.example.Request")));

        // When
        CatalogRegistrationOutcome outcome = this.repository.register(definition);

        // Then
        assertThat(outcome).isEqualTo(CatalogRegistrationOutcome.CREATED);
        assertThat(count("gw_service")).isEqualTo(1);
        assertThat(count("gw_service_operation")).isEqualTo(2);
        assertThat(count("gw_service_operation_parameter")).isEqualTo(2);
        assertThat(serviceRow("activity-service")).containsEntry("APPLICATION_NAME", "activity-application")
                .containsEntry("PROTOCOL", "dubbo")
                .containsEntry("NAMESPACE", "production")
                .containsEntry("SERVICE_VERSION", "1.0.0")
                .containsEntry("STATUS", "ACTIVE")
                .containsEntry("REVISION", 3L);
    }

    @Test
    void shouldTreatSameDefinitionWithDifferentOperationOrderAsUnchanged() {
        // Given
        ServiceOperation query = operation(
                "activity-query", "activity-service", "query", "name", "java.lang.String");
        ServiceOperation create = operation(
                "activity-create", "activity-service", "create", "request", "cn.example.Request");
        ServiceDefinition first = serviceDefinition(
                "activity-service", "activity-application", List.of(query, create));
        ServiceDefinition retried = serviceDefinition(
                "activity-service", "activity-application", List.of(create, query));
        assertThat(this.repository.register(first)).isEqualTo(CatalogRegistrationOutcome.CREATED);

        // When
        CatalogRegistrationOutcome outcome = this.repository.register(retried);

        // Then
        assertThat(outcome).isEqualTo(CatalogRegistrationOutcome.UNCHANGED);
        assertThat(count("gw_service")).isEqualTo(1);
        assertThat(count("gw_service_operation")).isEqualTo(2);
        assertThat(count("gw_service_operation_parameter")).isEqualTo(2);
    }

    @Test
    void shouldRegisterSameDefinitionIdempotentlyWhenRequestsRace() throws Exception {
        // Given
        ServiceDefinition definition = serviceDefinition("activity-service", "activity-application", List.of(
                operation("activity-query", "activity-service", "query", "name", "java.lang.String")));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            CompletableFuture<CatalogRegistrationOutcome> first = registerAsync(definition, executor, ready, start);
            CompletableFuture<CatalogRegistrationOutcome> second = registerAsync(definition, executor, ready, start);
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();

            // When
            start.countDown();
            CatalogRegistrationOutcome firstOutcome = first.get(5, TimeUnit.SECONDS);
            CatalogRegistrationOutcome secondOutcome = second.get(5, TimeUnit.SECONDS);

            // Then
            assertThat(List.of(firstOutcome, secondOutcome)).containsExactlyInAnyOrder(
                    CatalogRegistrationOutcome.CREATED, CatalogRegistrationOutcome.UNCHANGED);
            assertThat(count("gw_service")).isEqualTo(1);
            assertThat(count("gw_service_operation")).isEqualTo(1);
            assertThat(count("gw_service_operation_parameter")).isEqualTo(1);
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void shouldRejectDifferentMetadataForExistingServiceId() {
        // Given
        ServiceDefinition existing = serviceDefinition("activity-service", "activity-application", List.of());
        ServiceDefinition conflicting = serviceDefinition("activity-service", "other-application", List.of());
        this.repository.register(existing);

        // When & Then
        assertThatThrownBy(() -> this.repository.register(conflicting))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.responseCode()).isEqualTo(CatalogResponseCode.SERVICE_DEFINITION_CONFLICT);
                    assertThat(exception.arguments()).containsExactly("activity-service");
                });
        assertThat(serviceRow("activity-service")).containsEntry("APPLICATION_NAME", "activity-application");
    }

    @Test
    void shouldRejectDifferentOperationForExistingService() {
        // Given
        ServiceDefinition existing = serviceDefinition("activity-service", "activity-application", List.of(
                operation("activity-query", "activity-service", "query", "name", "java.lang.String")));
        ServiceDefinition conflicting = serviceDefinition("activity-service", "activity-application", List.of(
                operation("activity-query", "activity-service", "find", "name", "java.lang.String")));
        this.repository.register(existing);

        // When & Then
        assertThatThrownBy(() -> this.repository.register(conflicting))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.responseCode()).isEqualTo(CatalogResponseCode.OPERATION_DEFINITION_CONFLICT);
                    assertThat(exception.arguments()).containsExactly("activity-query");
                });
        assertThat(operationName("activity-query")).isEqualTo("query");
    }

    @Test
    void shouldRollBackNewServiceWhenOperationIdBelongsToAnotherService() {
        // Given
        ServiceDefinition existing = serviceDefinition("activity-service", "activity-application", List.of(
                operation("shared-operation", "activity-service", "query", "name", "java.lang.String")));
        ServiceDefinition conflicting = serviceDefinition("order-service", "order-application", List.of(
                operation("shared-operation", "order-service", "find", "id", "java.lang.Long")));
        this.repository.register(existing);

        // When & Then
        assertThatThrownBy(() -> this.repository.register(conflicting))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.responseCode()).isEqualTo(CatalogResponseCode.OPERATION_DEFINITION_CONFLICT);
                    assertThat(exception.arguments()).containsExactly("shared-operation");
                });
        assertThat(serviceCount("order-service")).isZero();
        assertThat(count("gw_service")).isEqualTo(1);
        assertThat(count("gw_service_operation")).isEqualTo(1);
        assertThat(count("gw_service_operation_parameter")).isEqualTo(1);
    }

    @Test
    void shouldRejectNullServiceDefinitionBeforeDatabaseAccess() {
        // When & Then
        assertThatThrownBy(() -> this.repository.register(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("serviceDefinition must not be null");
        assertThat(count("gw_service")).isZero();
    }

    private int count(String tableName) {
        return this.jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Integer.class);
    }

    private int serviceCount(String serviceId) {
        return this.jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM gw_service WHERE service_id = ?", Integer.class, serviceId);
    }

    private Map<String, Object> serviceRow(String serviceId) {
        return this.jdbcTemplate.queryForMap("SELECT * FROM gw_service WHERE service_id = ?", serviceId);
    }

    private String operationName(String operationId) {
        return this.jdbcTemplate.queryForObject(
                "SELECT operation_name FROM gw_service_operation WHERE operation_id = ?", String.class, operationId);
    }

    private CompletableFuture<CatalogRegistrationOutcome> registerAsync(
            ServiceDefinition definition, ExecutorService executor, CountDownLatch ready, CountDownLatch start) {
        return CompletableFuture.supplyAsync(() -> {
            ready.countDown();
            awaitStart(start);
            return this.repository.register(definition);
        }, executor);
    }

    private void awaitStart(CountDownLatch start) {
        try {
            if (!start.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("timed out while waiting to start catalog registration");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("catalog registration was interrupted", exception);
        }
    }

    private static ServiceDefinition serviceDefinition(
            String serviceId, String applicationName, List<ServiceOperation> operations) {
        return new ServiceDefinition(
                serviceId,
                applicationName,
                "dubbo",
                "production",
                "1.0.0",
                ServiceCatalogStatus.ACTIVE,
                3L,
                operations);
    }

    private static ServiceOperation operation(
            String operationId, String serviceId, String operationName, String parameterName, String typeName) {
        return new ServiceOperation(
                operationId,
                serviceId,
                "cn.example.ActivityService",
                operationName,
                ServiceCatalogStatus.ACTIVE,
                2L,
                List.of(new ServiceOperationParameter(0, parameterName, typeName)));
    }
}
