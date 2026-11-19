package com.lavyoung.gateweave.center.route.application;

import com.lavyoung.gateweave.center.route.domain.RouteAuthenticationRequirement;
import com.lavyoung.gateweave.center.route.domain.RouteDefinition;
import com.lavyoung.gateweave.center.route.domain.RouteDefinitionRepository;
import com.lavyoung.gateweave.center.route.domain.RouteStatus;
import com.lavyoung.gateweave.center.shared.pagination.PageRequest;
import com.lavyoung.gateweave.center.shared.pagination.PageResult;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class DefaultRouteQueryServiceTests {

    @Test
    void shouldQueryCountAndRequestedPage() {
        // Given
        RouteDefinitionRepository repository = mock(RouteDefinitionRepository.class);
        DefaultRouteQueryService service = new DefaultRouteQueryService(repository);
        PageRequest pageRequest = new PageRequest(2, 2);
        RoutePageQuery query = new RoutePageQuery("default", pageRequest);
        RouteDefinition route = route("route-three", "/three");
        when(repository.countByGatewayGroupId("default")).thenReturn(3L);
        when(repository.findPageByGatewayGroupId("default", 2L, 2)).thenReturn(List.of(route));

        // When
        PageResult<RouteDefinition> page = service.query(query);

        // Then
        assertThat(page.content()).containsExactly(route);
        assertThat(page.pageRequest()).isSameAs(pageRequest);
        assertThat(page.totalElements()).isEqualTo(3);
        assertThat(page.totalPages()).isEqualTo(2);
        verify(repository).countByGatewayGroupId("default");
        verify(repository).findPageByGatewayGroupId("default", 2L, 2);
    }

    @Test
    void shouldReturnEmptyPageWithoutLoadingRoutesWhenGroupHasNoRoutes() {
        // Given
        RouteDefinitionRepository repository = mock(RouteDefinitionRepository.class);
        DefaultRouteQueryService service = new DefaultRouteQueryService(repository);
        RoutePageQuery query = new RoutePageQuery("empty", new PageRequest(1, 20));
        when(repository.countByGatewayGroupId("empty")).thenReturn(0L);

        // When
        PageResult<RouteDefinition> page = service.query(query);

        // Then
        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isZero();
        assertThat(page.totalPages()).isZero();
        verify(repository).countByGatewayGroupId("empty");
        verify(repository, never()).findPageByGatewayGroupId("empty", 0L, 20);
    }

    @Test
    void shouldPreserveTotalWhenRequestedPageIsBeyondLastPage() {
        // Given
        RouteDefinitionRepository repository = mock(RouteDefinitionRepository.class);
        DefaultRouteQueryService service = new DefaultRouteQueryService(repository);
        RoutePageQuery query = new RoutePageQuery("default", new PageRequest(5, 20));
        when(repository.countByGatewayGroupId("default")).thenReturn(3L);
        when(repository.findPageByGatewayGroupId("default", 80L, 20)).thenReturn(List.of());

        // When
        PageResult<RouteDefinition> page = service.query(query);

        // Then
        assertThat(page.content()).isEmpty();
        assertThat(page.pageRequest().pageNumber()).isEqualTo(5);
        assertThat(page.totalElements()).isEqualTo(3);
        assertThat(page.totalPages()).isEqualTo(1);
    }

    @Test
    void shouldRejectMissingDependenciesAndQuery() {
        // Given
        RouteDefinitionRepository repository = mock(RouteDefinitionRepository.class);
        DefaultRouteQueryService service = new DefaultRouteQueryService(repository);

        // When & Then
        assertThatThrownBy(() -> new DefaultRouteQueryService(null)).isInstanceOf(NullPointerException.class)
                .hasMessage("routeRepository must not be null");
        assertThatThrownBy(() -> service.query(null)).isInstanceOf(NullPointerException.class)
                .hasMessage("query must not be null");
    }

    private static RouteDefinition route(String routeId, String path) {
        return new RouteDefinition(routeId, "default", path, "GET", "activity-query",
                RouteAuthenticationRequirement.REQUIRED, Duration.ofSeconds(3), Map.of(), Map.of(), RouteStatus.ENABLED,
                0L, List.of());
    }
}
