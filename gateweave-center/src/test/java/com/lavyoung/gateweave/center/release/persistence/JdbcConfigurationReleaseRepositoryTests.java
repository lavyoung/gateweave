package com.lavyoung.gateweave.center.release.persistence;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.lavyoung.gateweave.center.GateWeaveCenterApplication;
import com.lavyoung.gateweave.center.gateway.domain.exception.GatewayGroupNotFoundException;
import com.lavyoung.gateweave.center.release.application.ConfigurationReleaseRepository;
import com.lavyoung.gateweave.contract.configuration.GatewayConfigurationSnapshot;
import com.lavyoung.gateweave.contract.configuration.InvocationParameterConfiguration;
import com.lavyoung.gateweave.contract.configuration.InvocationTargetConfiguration;
import com.lavyoung.gateweave.contract.configuration.RouteAuthenticationMode;
import com.lavyoung.gateweave.contract.configuration.RouteConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(
        classes = GateWeaveCenterApplication.class,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:configuration-release-repository;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "spring.datasource.username=sa",
                "spring.datasource.password="
        })
class JdbcConfigurationReleaseRepositoryTests {

    private static final Instant FIRST_PUBLISHED_AT = Instant.parse("2026-11-02T10:15:30Z");
    private static final Instant SECOND_PUBLISHED_AT = Instant.parse("2026-11-02T10:16:30Z");

    private final ConfigurationReleaseRepository repository;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    JdbcConfigurationReleaseRepositoryTests(
            ConfigurationReleaseRepository repository, JdbcTemplate jdbcTemplate) {
        this.repository = repository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @BeforeEach
    void deleteConfigurationReleases() {
        this.jdbcTemplate.update("DELETE FROM gw_config_release");
    }

    @Test
    void shouldPublishAndReadImmutableConfigurationSnapshot() {
        // Given
        List<RouteConfiguration> routes = List.of(route());

        // When
        GatewayConfigurationSnapshot published = this.repository.publish("default", routes, FIRST_PUBLISHED_AT);
        Optional<GatewayConfigurationSnapshot> loaded = this.repository.findLatestPublished("default");

        // Then
        assertThat(published.gatewayGroupId()).isEqualTo("default");
        assertThat(published.version()).isEqualTo(1);
        assertThat(published.publishedAt()).isEqualTo(FIRST_PUBLISHED_AT);
        assertThat(published.checksum()).matches("sha256:[0-9a-f]{64}");
        assertThat(published.routes()).containsExactlyElementsOf(routes);
        assertThat(loaded).contains(published);
        assertThat(releaseCount()).isEqualTo(1);
    }

    @Test
    void shouldIncrementVersionAndReturnLatestPublishedSnapshot() {
        // Given
        GatewayConfigurationSnapshot first = this.repository.publish(
                "default", List.of(), FIRST_PUBLISHED_AT);

        // When
        GatewayConfigurationSnapshot second = this.repository.publish(
                "default", List.of(route()), SECOND_PUBLISHED_AT);
        GatewayConfigurationSnapshot latest = this.repository.findLatestPublished("default").orElseThrow();

        // Then
        assertThat(first.version()).isEqualTo(1);
        assertThat(second.version()).isEqualTo(2);
        assertThat(latest).isEqualTo(second);
        assertThat(releaseCount()).isEqualTo(2);
    }

    @Test
    void shouldReturnEmptyWhenGatewayGroupHasNoPublishedSnapshot() {
        // When
        Optional<GatewayConfigurationSnapshot> snapshot = this.repository.findLatestPublished("default");

        // Then
        assertThat(snapshot).isEmpty();
    }

    @Test
    void shouldRejectPublicationForMissingGatewayGroup() {
        // When & Then
        assertThatThrownBy(() -> this.repository.publish("missing-group", List.of(), FIRST_PUBLISHED_AT))
                .isInstanceOfSatisfying(GatewayGroupNotFoundException.class,
                        exception -> assertThat(exception.gatewayGroupId()).isEqualTo("missing-group"));
        assertThat(releaseCount()).isZero();
    }

    @Test
    void shouldRejectSnapshotWhoseStoredContentDoesNotMatchChecksum() {
        // Given
        this.repository.publish("default", List.of(route()), FIRST_PUBLISHED_AT);
        this.jdbcTemplate.update("""
                UPDATE gw_config_release
                SET snapshot_content = '[]'
                WHERE gateway_group_id = 'default'
                """);

        // When & Then
        assertThatThrownBy(() -> this.repository.findLatestPublished("default"))
                .isInstanceOf(DataRetrievalFailureException.class)
                .hasMessage("Configuration release checksum does not match stored content");
    }

    @Test
    void shouldAllocateUniqueVersionsWhenSameGatewayGroupIsPublishedConcurrently() throws Exception {
        // Given
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            CompletableFuture<GatewayConfigurationSnapshot> first = publishAsync(
                    List.of(route()), FIRST_PUBLISHED_AT, executor, ready, start);
            CompletableFuture<GatewayConfigurationSnapshot> second = publishAsync(
                    List.of(), SECOND_PUBLISHED_AT, executor, ready, start);
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();

            // When
            start.countDown();
            GatewayConfigurationSnapshot firstSnapshot = first.get(5, TimeUnit.SECONDS);
            GatewayConfigurationSnapshot secondSnapshot = second.get(5, TimeUnit.SECONDS);

            // Then
            assertThat(List.of(firstSnapshot.version(), secondSnapshot.version()))
                    .containsExactlyInAnyOrder(1L, 2L);
            assertThat(releaseCount()).isEqualTo(2);
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    private CompletableFuture<GatewayConfigurationSnapshot> publishAsync(
            List<RouteConfiguration> routes, Instant publishedAt, ExecutorService executor,
            CountDownLatch ready, CountDownLatch start) {
        return CompletableFuture.supplyAsync(() -> {
            ready.countDown();
            awaitStart(start);
            return this.repository.publish("default", routes, publishedAt);
        }, executor);
    }

    private void awaitStart(CountDownLatch start) {
        try {
            if (!start.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("timed out while waiting to publish configuration");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("configuration publication was interrupted", exception);
        }
    }

    private int releaseCount() {
        return this.jdbcTemplate.queryForObject("SELECT COUNT(*) FROM gw_config_release", Integer.class);
    }

    private static RouteConfiguration route() {
        InvocationTargetConfiguration target = new InvocationTargetConfiguration(
                "dubbo", "cn.example.ActivityBooth", "sayHi",
                List.of(new InvocationParameterConfiguration("name", "java.lang.String")), Duration.ofSeconds(3),
                Map.of("version", "1.0.0"));
        return new RouteConfiguration(
                "route-say-hi", "/sayHi", "GET", "activity-service", RouteAuthenticationMode.REQUIRED,
                target, Map.of("filter", "logging"));
    }
}
