package com.lavyoung.gateweave.center.gateway.application;

import com.lavyoung.gateweave.center.gateway.application.service.DefaultGatewayNodeQueryService;
import com.lavyoung.gateweave.center.gateway.domain.GatewayNode;
import com.lavyoung.gateweave.center.gateway.domain.GatewayNodeRegistration;
import com.lavyoung.gateweave.center.gateway.domain.GatewayNodeStatus;
import com.lavyoung.gateweave.center.shared.pagination.PageRequest;
import com.lavyoung.gateweave.center.shared.pagination.PageResult;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class DefaultGatewayNodeQueryServiceTests {

    private static final Instant HEARTBEAT_AT = Instant.parse("2026-11-17T02:20:00Z");

    @Test
    void shouldQueryCountAndRequestedPage() {
        // Given
        GatewayNodeQueryRepository repository = mock(GatewayNodeQueryRepository.class);
        DefaultGatewayNodeQueryService service = new DefaultGatewayNodeQueryService(repository);
        PageRequest pageRequest = new PageRequest(2, 2);
        GatewayNodePageQuery query =
                new GatewayNodePageQuery("", "default", Set.of(GatewayNodeStatus.ONLINE), pageRequest);
        GatewayNode node = node("node-3");
        when(repository.countMatching(query)).thenReturn(3L);
        when(repository.findMatchingPage(query)).thenReturn(List.of(node));

        // When
        PageResult<GatewayNode> page = service.query(query);

        // Then
        assertThat(page.content()).containsExactly(node);
        assertThat(page.pageRequest()).isSameAs(pageRequest);
        assertThat(page.totalElements()).isEqualTo(3);
        assertThat(page.totalPages()).isEqualTo(2);
        verify(repository).countMatching(query);
        verify(repository).findMatchingPage(query);
    }

    @Test
    void shouldReturnEmptyPageWithoutLoadingNodesWhenNoNodeMatches() {
        // Given
        GatewayNodeQueryRepository repository = mock(GatewayNodeQueryRepository.class);
        DefaultGatewayNodeQueryService service = new DefaultGatewayNodeQueryService(repository);
        GatewayNodePageQuery query =
                new GatewayNodePageQuery("missing", "", Set.of(), new PageRequest(1, 20));
        when(repository.countMatching(query)).thenReturn(0L);

        // When
        PageResult<GatewayNode> page = service.query(query);

        // Then
        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isZero();
        assertThat(page.totalPages()).isZero();
        verify(repository).countMatching(query);
        verify(repository, never()).findMatchingPage(query);
    }

    @Test
    void shouldPreserveTotalWhenRequestedPageIsBeyondLastPage() {
        // Given
        GatewayNodeQueryRepository repository = mock(GatewayNodeQueryRepository.class);
        DefaultGatewayNodeQueryService service = new DefaultGatewayNodeQueryService(repository);
        GatewayNodePageQuery query = new GatewayNodePageQuery("", "", Set.of(), new PageRequest(5, 20));
        when(repository.countMatching(query)).thenReturn(3L);
        when(repository.findMatchingPage(query)).thenReturn(List.of());

        // When
        PageResult<GatewayNode> page = service.query(query);

        // Then
        assertThat(page.content()).isEmpty();
        assertThat(page.pageRequest().pageNumber()).isEqualTo(5);
        assertThat(page.totalElements()).isEqualTo(3);
        assertThat(page.totalPages()).isEqualTo(1);
    }

    @Test
    void shouldRejectMissingDependencyAndQuery() {
        // Given
        GatewayNodeQueryRepository repository = mock(GatewayNodeQueryRepository.class);
        DefaultGatewayNodeQueryService service = new DefaultGatewayNodeQueryService(repository);

        // When & Then
        assertThatThrownBy(() -> new DefaultGatewayNodeQueryService(null)).isInstanceOf(NullPointerException.class)
                .hasMessage("queryRepository must not be null");
        assertThatThrownBy(() -> service.query(null)).isInstanceOf(NullPointerException.class)
                .hasMessage("query must not be null");
        verifyNoInteractions(repository);
    }

    private static GatewayNode node(String nodeId) {
        GatewayNodeRegistration registration = new GatewayNodeRegistration(nodeId, "default", "Gateway Node",
                URI.create("http://127.0.0.1:8080/" + nodeId));
        return new GatewayNode(registration, GatewayNodeStatus.ONLINE, UUID.randomUUID(), HEARTBEAT_AT, 3);
    }
}
