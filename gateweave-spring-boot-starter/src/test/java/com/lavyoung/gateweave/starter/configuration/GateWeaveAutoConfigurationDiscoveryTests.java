package com.lavyoung.gateweave.starter.configuration;

import com.lavyoung.gateweave.starter.discovery.ServiceMetadataBeanPostProcessor;
import com.lavyoung.gateweave.starter.registration.ServiceMetadataRegistry;
import com.lavyoung.gateweave.starter.registration.ServiceRegistrationClient;
import com.lavyoung.gateweave.starter.registration.ServiceRegistrationRunner;
import com.lavyoung.gateweave.starter.registration.http.ServiceRegistrationHttpApi;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class GateWeaveAutoConfigurationDiscoveryTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestApplication.class)
            .withPropertyValues("spring.application.name=discovery-test-application");

    @Test
    void shouldDiscoverGateWeaveAutoConfigurationFromImports() {
        this.contextRunner.run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(ServiceMetadataBeanPostProcessor.class)
                    .hasSingleBean(ServiceMetadataRegistry.class)
                    .doesNotHaveBean(ServiceRegistrationClient.class)
                    .doesNotHaveBean(ServiceRegistrationRunner.class);
            assertThat(context)
                    .hasBean(GateWeaveServiceMetadataAutoConfiguration.SERVICE_METADATA_CONSUMER_BEAN_NAME);
        });
    }

    @Test
    void shouldDiscoverRegistrationAutoConfigurationWhenControlPlaneAddressIsConfigured() {
        this.contextRunner.withPropertyValues("gateweave.registration.base-uri=http://127.0.0.1:8080")
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(GateWeaveRegistrationProperties.class)
                            .hasSingleBean(ServiceRegistrationHttpApi.class)
                            .hasSingleBean(ServiceRegistrationClient.class)
                            .hasSingleBean(ServiceRegistrationRunner.class);
                    assertThat(context).hasBean(
                            GateWeaveServiceRegistrationAutoConfiguration.REGISTRATION_HTTP_CLIENT_BEAN_NAME);
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class TestApplication {
    }
}
