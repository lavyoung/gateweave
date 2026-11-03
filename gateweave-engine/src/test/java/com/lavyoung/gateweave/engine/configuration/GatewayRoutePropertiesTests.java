package com.lavyoung.gateweave.engine.configuration;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import com.lavyoung.gateweave.contract.configuration.RouteAuthenticationMode;
import com.lavyoung.gateweave.engine.application.mapper.GatewayRouteConfigurationMapper;
import com.lavyoung.gateweave.engine.domain.route.RouteAuthenticationPolicy;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayRoutePropertiesTests {

    @Test
    void shouldBindAuthenticationModeAndConvertItToDomainPolicy() throws IOException {
        // Given
        var environment = new StandardEnvironment();
        var applicationYaml = new ClassPathResource("application.yml");
        var propertySources = new YamlPropertySourceLoader().load("application", applicationYaml);
        propertySources.forEach(environment.getPropertySources()::addLast);

        // When
        var properties = Binder.get(environment).bind("gateweave.routing", GatewayRouteProperties.class)
                .orElseThrow(() -> new AssertionError("gateweave.routing properties must be present"));
        var configuration = properties.routes().get(0);
        var route = new GatewayRouteConfigurationMapper().toDomain(configuration);

        // Then
        assertThat(configuration.authenticationMode()).isEqualTo(RouteAuthenticationMode.ANONYMOUS);
        assertThat(route.authenticationPolicy()).isEqualTo(RouteAuthenticationPolicy.ANONYMOUS);
    }
}
