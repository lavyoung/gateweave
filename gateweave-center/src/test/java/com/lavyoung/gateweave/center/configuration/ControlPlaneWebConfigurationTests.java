package com.lavyoung.gateweave.center.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ControlPlaneWebConfigurationTests {

    private static final List<String> ALLOWED_ORIGINS = List.of("http://localhost:5173");
    private static final Duration CORS_MAX_AGE = Duration.ofMinutes(30);
    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withUserConfiguration(ControlPlaneWebConfiguration.class);

    @Test
    void shouldApplyDefaultPrefixToRestControllers() {
        this.contextRunner.run(context -> {
            ControlPlaneWebProperties properties = context.getBean(ControlPlaneWebProperties.class);
            ControlPlaneWebConfiguration configuration = context.getBean(ControlPlaneWebConfiguration.class);
            TestPathMatchConfigurer configurer = new TestPathMatchConfigurer();

            configuration.configurePathMatch(configurer);

            Map<String, Predicate<Class<?>>> pathPrefixes = configurer.pathPrefixes();
            assertThat(properties.apiPrefix()).isEqualTo("/api/v1");
            assertThat(properties.allowedOrigins()).containsExactly("http://localhost:5173",
                    "http://127.0.0.1:5173");
            assertThat(properties.corsMaxAge()).isEqualTo(CORS_MAX_AGE);
            assertThat(pathPrefixes).containsOnlyKeys("/api/v1");
            assertThat(pathPrefixes.get("/api/v1").test(TestRestController.class)).isTrue();
            assertThat(pathPrefixes.get("/api/v1").test(PlainType.class)).isFalse();
        });
    }

    @Test
    void shouldBindCustomApiPrefix() {
        this.contextRunner.withPropertyValues("gateweave.web.api-prefix=/control-plane/v1",
                "gateweave.web.allowed-origins=https://admin.example.com", "gateweave.web.cors-max-age=10m")
                .run(context -> {
            ControlPlaneWebProperties properties = context.getBean(ControlPlaneWebProperties.class);

            assertThat(properties.apiPrefix()).isEqualTo("/control-plane/v1");
            assertThat(properties.allowedOrigins()).containsExactly("https://admin.example.com");
            assertThat(properties.corsMaxAge()).isEqualTo(Duration.ofMinutes(10));
        });
    }

    @Test
    void shouldRejectInvalidApiPrefix() {
        assertThatThrownBy(() -> new ControlPlaneWebProperties("api/v1", ALLOWED_ORIGINS, CORS_MAX_AGE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("apiPrefix must start with '/' and must not end with '/'");
        assertThatThrownBy(() -> new ControlPlaneWebProperties("/api/v1/", ALLOWED_ORIGINS, CORS_MAX_AGE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("apiPrefix must start with '/' and must not end with '/'");
        assertThatThrownBy(() -> new ControlPlaneWebProperties("/api//v1", ALLOWED_ORIGINS, CORS_MAX_AGE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("apiPrefix must be a normalized path without query or fragment");
    }

    @Test
    void shouldRejectInvalidCorsPolicy() {
        assertThatThrownBy(() -> new ControlPlaneWebProperties("/api/v1", List.of("*"), CORS_MAX_AGE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("allowedOrigins must contain only HTTP origins without paths");
        assertThatThrownBy(() -> new ControlPlaneWebProperties("/api/v1",
                List.of("https://admin.example.com/path"), CORS_MAX_AGE)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("allowedOrigins must contain only HTTP origins without paths");
        assertThatThrownBy(() -> new ControlPlaneWebProperties("/api/v1", ALLOWED_ORIGINS, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("corsMaxAge must be greater than zero");
    }

    @RestController
    private static class TestRestController {
    }

    private static class PlainType {
    }

    private static class TestPathMatchConfigurer extends PathMatchConfigurer {

        private Map<String, Predicate<Class<?>>> pathPrefixes() {
            return getPathPrefixes();
        }
    }
}
