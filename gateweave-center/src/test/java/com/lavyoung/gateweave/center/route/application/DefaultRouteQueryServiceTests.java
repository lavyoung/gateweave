package com.lavyoung.gateweave.center.route.application;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.lavyoung.gateweave.center.route.domain.RouteAuthenticationRequirement;
import com.lavyoung.gateweave.center.route.domain.RouteDefinition;
import com.lavyoung.gateweave.center.route.domain.RouteStatus;
import com.lavyoung.gateweave.center.route.domain.RouteDefinitionRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultRouteQueryServiceTests {

    @Test
    void shouldQueryCountAndRequestedPage() {
        // Given
        RouteDefinitionRepository repository = mock(RouteDefinitionRepository.class);
        DefaultRouteQueryService service = new DefaultRouteQueryService(repository);
        RoutePageQuery query = new RoutePageQuery("default", 2, 2);
        RouteDefinition route = route("route-three", "/three");
        when(repository.countByGatewayGroupId("default")).thenReturn(3L);
        when(repository.findPageByGatewayGroupId("default", 2L, 2)).thenReturn(List.of(route));

        // When
        RoutePage page = service.query(query);

        // Then
        assertThat(page.routes()).containsExactly(route);
        assertThat(page.pageNumber()).isEqualTo(2);
        assertThat(page.pageSize()).isEqualTo(2);
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
        RoutePageQuery query = new RoutePageQuery("empty", 1, 20);
        when(repository.countByGatewayGroupId("empty")).thenReturn(0L);

        // When
        RoutePage page = service.query(query);

        // Then
        assertThat(page.routes()).isEmpty();
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
        RoutePageQuery query = new RoutePageQuery("default", 5, 20);
        when(repository.countByGatewayGroupId("default")).thenReturn(3L);
        when(repository.findPageByGatewayGroupId("default", 80L, 20)).thenReturn(List.of());

        // When
        RoutePage page = service.query(query);

        // Then
        assertThat(page.routes()).isEmpty();
        assertThat(page.pageNumber()).isEqualTo(5);
        assertThat(page.totalElements()).isEqualTo(3);
        assertThat(page.totalPages()).isEqualTo(1);
    }

    @Test
    void shouldRejectMissingDependenciesAndQuery() {
        // Given
        RouteDefinitionRepository repository = mock(RouteDefinitionRepository.class);
        DefaultRouteQueryService service = new DefaultRouteQueryService(repository);

        // When & Then
        assertThatThrownBy(() -> new DefaultRouteQueryService(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("routeRepository must not be null");
        assertThatThrownBy(() -> service.query(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("query must not be null");
    }

    private static RouteDefinition route(String routeId, String path) {
        return new RouteDefinition(routeId, "default", path, "GET", "activity-query",
                RouteAuthenticationRequirement.REQUIRED, Duration.ofSeconds(3), Map.of(), Map.of(),
                RouteStatus.ENABLED, 0L, List.of());
    }
}
