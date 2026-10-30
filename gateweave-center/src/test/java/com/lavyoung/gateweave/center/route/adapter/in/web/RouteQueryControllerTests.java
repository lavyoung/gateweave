package com.lavyoung.gateweave.center.route.adapter.in.web;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.lavyoung.gateweave.center.route.application.port.in.QueryRoutePageUseCase;
import com.lavyoung.gateweave.center.route.application.query.RoutePage;
import com.lavyoung.gateweave.center.route.application.query.RoutePageQuery;
import com.lavyoung.gateweave.center.route.domain.RouteAuthenticationRequirement;
import com.lavyoung.gateweave.center.route.domain.RouteDefinition;
import com.lavyoung.gateweave.center.route.domain.RouteParameterMapping;
import com.lavyoung.gateweave.center.route.domain.RouteStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RouteQueryController.class)
@MockBean(QueryRoutePageUseCase.class)
class RouteQueryControllerTests {

    private final MockMvc mockMvc;
    private final QueryRoutePageUseCase queryRoutePageUseCase;

    @Autowired
    RouteQueryControllerTests(MockMvc mockMvc, QueryRoutePageUseCase queryRoutePageUseCase) {
        this.mockMvc = mockMvc;
        this.queryRoutePageUseCase = queryRoutePageUseCase;
    }

    @AfterEach
    void resetUseCase() {
        reset(this.queryRoutePageUseCase);
    }

    @Test
    void shouldQueryRoutesWithDefaultPagination() throws Exception {
        // Given
        RouteDefinition route = route();
        when(this.queryRoutePageUseCase.query(any(RoutePageQuery.class)))
                .thenReturn(new RoutePage(List.of(route), 1, 20, 1));

        // When & Then
        this.mockMvc.perform(get("/api/v1/gateway-groups/default/routes"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.pageNumber").value(1))
                .andExpect(jsonPath("$.pageSize").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.routes[0].routeId").value("route-query"))
                .andExpect(jsonPath("$.routes[0].invocationTimeoutMillis").value(2500))
                .andExpect(jsonPath("$.routes[0].parameterMappings[0].requestName").value("name"));

        ArgumentCaptor<RoutePageQuery> queryCaptor = ArgumentCaptor.forClass(RoutePageQuery.class);
        verify(this.queryRoutePageUseCase).query(queryCaptor.capture());
        assertThat(queryCaptor.getValue()).isEqualTo(new RoutePageQuery("default", 1, 20));
    }

    @Test
    void shouldQueryRoutesWithRequestedPagination() throws Exception {
        // Given
        when(this.queryRoutePageUseCase.query(any(RoutePageQuery.class)))
                .thenReturn(new RoutePage(List.of(), 2, 5, 6));

        // When & Then
        this.mockMvc.perform(get("/api/v1/gateway-groups/test/routes")
                        .param("pageNumber", "2")
                        .param("pageSize", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routes").isEmpty())
                .andExpect(jsonPath("$.pageNumber").value(2))
                .andExpect(jsonPath("$.pageSize").value(5))
                .andExpect(jsonPath("$.totalElements").value(6))
                .andExpect(jsonPath("$.totalPages").value(2));

        verify(this.queryRoutePageUseCase).query(new RoutePageQuery("test", 2, 5));
    }

    @Test
    void shouldRejectInvalidPaginationBeforeCallingUseCase() throws Exception {
        // When & Then
        this.mockMvc.perform(get("/api/v1/gateway-groups/default/routes").param("pageNumber", "0"))
                .andExpect(status().isBadRequest());
        this.mockMvc.perform(get("/api/v1/gateway-groups/default/routes").param("pageSize", "101"))
                .andExpect(status().isBadRequest());

        verify(this.queryRoutePageUseCase, never()).query(any(RoutePageQuery.class));
    }

    @Test
    void shouldRejectMissingUseCase() {
        // When & Then
        assertThatThrownBy(() -> new RouteQueryController(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("queryRoutePageUseCase must not be null");
    }

    private static RouteDefinition route() {
        return new RouteDefinition(
                "route-query", "default", "/activities", "GET", "activity-query",
                RouteAuthenticationRequirement.REQUIRED, Duration.ofMillis(2500),
                Map.of("version", "1.0.0"), Map.of("source", "center"), RouteStatus.ENABLED, 3L,
                List.of(new RouteParameterMapping(0, "name")));
    }
}
