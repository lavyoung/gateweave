package com.lavyoung.gateweave.engine.adapter.in.configuration;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.lavyoung.gateweave.contract.configuration.InvocationTargetConfiguration;
import com.lavyoung.gateweave.contract.configuration.RouteConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GatewayRouteConfigurationMapperTests {

    private static final InvocationTargetConfiguration TARGET_CONFIGURATION = new InvocationTargetConfiguration(
            "DUBBO", "cn.bugstack.gateway.rpc.IActivityBooth", "sayHi", List.of("java.lang.String"),
            Duration.ofSeconds(3), Map.of("version", "1.0.0"));

    private final GatewayRouteConfigurationMapper mapper = new GatewayRouteConfigurationMapper();

    @Test
    void shouldConvertRouteConfigurationToDomainRoute() {
        // Given
        var configuration = new RouteConfiguration(
                "route-say-hi", "/sayHi", "get", "activity-service", TARGET_CONFIGURATION,
                Map.of("filter", "logging"));

        // When
        var route = this.mapper.toDomain(configuration);

        // Then
        assertThat(route.routeId()).isEqualTo("route-say-hi");
        assertThat(route.path()).isEqualTo("/sayHi");
        assertThat(route.method()).isEqualTo("GET");
        assertThat(route.serviceId()).isEqualTo("activity-service");
        assertThat(route.attributes()).containsExactlyEntriesOf(Map.of("filter", "logging"));
        assertThat(route.target().protocol()).isEqualTo("dubbo");
        assertThat(route.target().service()).isEqualTo("cn.bugstack.gateway.rpc.IActivityBooth");
        assertThat(route.target().operation()).isEqualTo("sayHi");
        assertThat(route.target().parameterTypes()).containsExactly("java.lang.String");
        assertThat(route.target().timeout()).isEqualTo(Duration.ofSeconds(3));
        assertThat(route.target().attributes()).containsExactlyEntriesOf(Map.of("version", "1.0.0"));
    }

    @Test
    void shouldRejectMissingConfiguration() {
        // When & Then
        assertThatThrownBy(() -> this.mapper.toDomain(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("configuration must not be null");
    }

    @Test
    void shouldPropagateDomainValidationFailure() {
        // Given
        var configuration = new RouteConfiguration(
                "route-say-hi", "sayHi", "GET", "activity-service", TARGET_CONFIGURATION, Map.of());

        // When & Then
        assertThatThrownBy(() -> this.mapper.toDomain(configuration))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("path must start with '/'");
    }
}
