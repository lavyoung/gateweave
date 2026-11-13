package com.lavyoung.gateweave.starter.registration;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import com.lavyoung.gateweave.starter.metadata.ServiceMetadata;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServiceMetadataRegistryTests {

    @Test
    void shouldReturnMetadataSortedByServiceId() {
        // Given
        ServiceMetadataRegistry registry = new ServiceMetadataRegistry();

        // When
        registry.accept(metadata("payment-service", "1.0.0"));
        registry.accept(metadata("activity-service", "1.0.0"));

        // Then
        assertThat(registry.snapshot()).extracting(ServiceMetadata::serviceId)
                .containsExactly("activity-service", "payment-service");
    }

    @Test
    void shouldKeepIdenticalMetadataRegistrationIdempotent() {
        // Given
        ServiceMetadataRegistry registry = new ServiceMetadataRegistry();
        ServiceMetadata metadata = metadata("activity-service", "1.0.0");

        // When
        registry.accept(metadata);
        registry.accept(metadata);

        // Then
        assertThat(registry.snapshot()).containsExactly(metadata);
    }

    @Test
    void shouldRejectConflictingMetadataForSameService() {
        // Given
        ServiceMetadataRegistry registry = new ServiceMetadataRegistry();
        registry.accept(metadata("activity-service", "1.0.0"));

        // When & Then
        assertThatThrownBy(() -> registry.accept(metadata("activity-service", "2.0.0")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Conflicting service metadata discovered for serviceId: activity-service");
        assertThat(registry.snapshot()).containsExactly(metadata("activity-service", "1.0.0"));
    }

    @Test
    void shouldRejectMissingMetadata() {
        // Given
        ServiceMetadataRegistry registry = new ServiceMetadataRegistry();

        // When & Then
        assertThatThrownBy(() -> registry.accept(null)).isInstanceOf(NullPointerException.class)
                .hasMessage("metadata must not be null");
    }

    @Test
    void shouldReturnImmutableSnapshot() {
        // Given
        ServiceMetadataRegistry registry = new ServiceMetadataRegistry();
        registry.accept(metadata("activity-service", "1.0.0"));

        // When
        List<ServiceMetadata> snapshot = registry.snapshot();

        // Then
        assertThatThrownBy(snapshot::clear).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldCollectDistinctServicesConcurrently() {
        // Given
        ServiceMetadataRegistry registry = new ServiceMetadataRegistry();
        ExecutorService executorService = Executors.newFixedThreadPool(4);

        try {
            // When
            List<CompletableFuture<Void>> futures = IntStream.range(0, 20)
                    .mapToObj(index -> CompletableFuture.runAsync(
                            () -> registry.accept(metadata("service-" + index, "1.0.0")), executorService))
                    .toList();
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();

            // Then
            assertThat(registry.snapshot()).hasSize(20);
        } finally {
            executorService.shutdownNow();
        }
    }

    private static ServiceMetadata metadata(String serviceId, String version) {
        return new ServiceMetadata(serviceId, "test-application", "dubbo", "", version, List.of());
    }
}
