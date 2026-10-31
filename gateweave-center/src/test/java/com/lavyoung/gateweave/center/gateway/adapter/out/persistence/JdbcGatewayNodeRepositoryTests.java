package com.lavyoung.gateweave.center.gateway.adapter.out.persistence;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.lavyoung.gateweave.center.GateWeaveCenterApplication;
import com.lavyoung.gateweave.center.gateway.domain.GatewayNode;
import com.lavyoung.gateweave.center.gateway.domain.GatewayNodeRegistration;
import com.lavyoung.gateweave.center.gateway.domain.GatewayNodeRegistrationOutcome;
import com.lavyoung.gateweave.center.gateway.domain.GatewayNodeRegistrationResult;
import com.lavyoung.gateweave.center.gateway.domain.GatewayNodeStatus;
import com.lavyoung.gateweave.center.gateway.domain.exception.GatewayGroupNotFoundException;
import com.lavyoung.gateweave.center.gateway.domain.exception.GatewayNodeAddressConflictException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(
        classes = GateWeaveCenterApplication.class,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:gateway-node-repository;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "spring.datasource.username=sa",
                "spring.datasource.password="
        })
class JdbcGatewayNodeRepositoryTests {

    private static final Instant FIRST_HEARTBEAT = Instant.parse("2026-10-31T12:00:00Z");
    private static final Instant SECOND_HEARTBEAT = Instant.parse("2026-10-31T12:05:00Z");

    private final JdbcGatewayNodeRepository repository;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    JdbcGatewayNodeRepositoryTests(JdbcGatewayNodeRepository repository, JdbcTemplate jdbcTemplate) {
        this.repository = repository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @BeforeEach
    void deleteGatewayNodes() {
        this.jdbcTemplate.update("DELETE FROM gw_gateway_node");
    }

    @Test
    void shouldRegisterNewGatewayNode() {
        // Given
        GatewayNode node = node("node-1", "Gateway Node 1", "http://127.0.0.1:8081", FIRST_HEARTBEAT);

        // When
        GatewayNodeRegistrationResult result = this.repository.registerOrRefresh(node);

        // Then
        assertThat(result.outcome()).isEqualTo(GatewayNodeRegistrationOutcome.REGISTERED);
        assertThat(result.node()).isEqualTo(node);
        assertThat(nodeCount("node-1")).isEqualTo(1);
    }

    @Test
    void shouldRefreshNodeWithoutResettingCurrentConfigVersion() {
        // Given
        GatewayNode firstNode = node("node-1", "Old Name", "http://127.0.0.1:8081", FIRST_HEARTBEAT);
        this.repository.registerOrRefresh(firstNode);
        this.jdbcTemplate.update("""
                UPDATE gw_gateway_node
                SET current_config_version = 7,
                    status = 'OFFLINE'
                WHERE node_id = 'node-1'
                """);
        GatewayNode refreshedNode = node(
                "node-1", "New Name", "http://127.0.0.1:8082", SECOND_HEARTBEAT);

        // When
        GatewayNodeRegistrationResult result = this.repository.registerOrRefresh(refreshedNode);

        // Then
        assertThat(result.outcome()).isEqualTo(GatewayNodeRegistrationOutcome.REFRESHED);
        assertThat(result.node().registration().nodeName()).isEqualTo("New Name");
        assertThat(result.node().registration().advertisedAddress())
                .isEqualTo(URI.create("http://127.0.0.1:8082"));
        assertThat(result.node().status()).isEqualTo(GatewayNodeStatus.ONLINE);
        assertThat(result.node().lastHeartbeatAt()).isEqualTo(SECOND_HEARTBEAT);
        assertThat(result.node().currentConfigVersion()).isEqualTo(7);
        assertThat(nodeCount("node-1")).isEqualTo(1);
    }

    @Test
    void shouldRejectAddressOwnedByAnotherNode() {
        // Given
        GatewayNode firstNode = node("node-1", "Gateway Node 1", "http://127.0.0.1:8081", FIRST_HEARTBEAT);
        GatewayNode conflictingNode = node(
                "node-2", "Gateway Node 2", "http://127.0.0.1:8081", SECOND_HEARTBEAT);
        this.repository.registerOrRefresh(firstNode);

        // When & Then
        assertThatThrownBy(() -> this.repository.registerOrRefresh(conflictingNode))
                .isInstanceOfSatisfying(GatewayNodeAddressConflictException.class,
                        exception -> assertThat(exception.advertisedAddress())
                                .isEqualTo(URI.create("http://127.0.0.1:8081")));
        assertThat(totalNodeCount()).isEqualTo(1);
    }

    @Test
    void shouldRejectNodeAssignedToMissingGatewayGroup() {
        // Given
        GatewayNodeRegistration registration = new GatewayNodeRegistration(
                "node-1", "missing-group", "Gateway Node 1", URI.create("http://127.0.0.1:8081"));
        GatewayNode node = GatewayNode.registered(registration, FIRST_HEARTBEAT);

        // When & Then
        assertThatThrownBy(() -> this.repository.registerOrRefresh(node))
                .isInstanceOfSatisfying(GatewayGroupNotFoundException.class,
                        exception -> assertThat(exception.gatewayGroupId()).isEqualTo("missing-group"));
        assertThat(totalNodeCount()).isZero();
    }

    @Test
    void shouldRegisterSameNodeIdempotentlyWhenRequestsRace() throws Exception {
        // Given
        GatewayNode node = node("node-1", "Gateway Node 1", "http://127.0.0.1:8081", FIRST_HEARTBEAT);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            CompletableFuture<GatewayNodeRegistrationResult> first = registerAsync(node, executor, ready, start);
            CompletableFuture<GatewayNodeRegistrationResult> second = registerAsync(node, executor, ready, start);
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();

            // When
            start.countDown();
            GatewayNodeRegistrationOutcome firstOutcome = first.get(5, TimeUnit.SECONDS).outcome();
            GatewayNodeRegistrationOutcome secondOutcome = second.get(5, TimeUnit.SECONDS).outcome();

            // Then
            assertThat(List.of(firstOutcome, secondOutcome)).containsExactlyInAnyOrder(
                    GatewayNodeRegistrationOutcome.REGISTERED,
                    GatewayNodeRegistrationOutcome.REFRESHED);
            assertThat(nodeCount("node-1")).isEqualTo(1);
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    private CompletableFuture<GatewayNodeRegistrationResult> registerAsync(
            GatewayNode node, ExecutorService executor, CountDownLatch ready, CountDownLatch start) {
        return CompletableFuture.supplyAsync(() -> {
            ready.countDown();
            awaitStart(start);
            return this.repository.registerOrRefresh(node);
        }, executor);
    }

    private void awaitStart(CountDownLatch start) {
        try {
            if (!start.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("timed out while waiting to start registration");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("gateway node registration was interrupted", exception);
        }
    }

    private int nodeCount(String nodeId) {
        return this.jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM gw_gateway_node WHERE node_id = ?", Integer.class, nodeId);
    }

    private int totalNodeCount() {
        return this.jdbcTemplate.queryForObject("SELECT COUNT(*) FROM gw_gateway_node", Integer.class);
    }

    private static GatewayNode node(String nodeId, String nodeName, String address, Instant heartbeatAt) {
        GatewayNodeRegistration registration = new GatewayNodeRegistration(
                nodeId, "default", nodeName, URI.create(address));
        return GatewayNode.registered(registration, heartbeatAt);
    }
}
