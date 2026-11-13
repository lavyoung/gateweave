package com.lavyoung.gateweave.starter.configuration;

import java.net.http.HttpClient;

import com.lavyoung.gateweave.contract.catalog.CatalogRegistrationOutcome;
import com.lavyoung.gateweave.starter.registration.ServiceMetadataRegistry;
import com.lavyoung.gateweave.starter.registration.ServiceRegistrationClient;
import com.lavyoung.gateweave.starter.registration.ServiceRegistrationRunner;
import com.lavyoung.gateweave.starter.registration.http.HttpServiceRegistrationClient;
import com.lavyoung.gateweave.starter.registration.http.ServiceRegistrationHttpApi;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class GateWeaveServiceRegistrationAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RestClientAutoConfiguration.class,
                    GateWeaveServiceMetadataAutoConfiguration.class,
                    GateWeaveServiceRegistrationAutoConfiguration.class))
            .withPropertyValues("spring.application.name=registration-test-application");

    @Test
    void shouldNotEnableRegistrationWithoutControlPlaneAddress() {
        this.contextRunner.run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(ServiceMetadataRegistry.class)
                    .doesNotHaveBean(GateWeaveRegistrationProperties.class)
                    .doesNotHaveBean(ServiceRegistrationHttpApi.class)
                    .doesNotHaveBean(ServiceRegistrationClient.class)
                    .doesNotHaveBean(ServiceRegistrationRunner.class);
            assertThat(context).doesNotHaveBean(
                    GateWeaveServiceRegistrationAutoConfiguration.REGISTRATION_HTTP_CLIENT_BEAN_NAME);
        });
    }

    @Test
    void shouldConfigureCompleteRegistrationChain() {
        this.contextRunner.withPropertyValues("gateweave.registration.base-uri=http://127.0.0.1:8080")
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(GateWeaveRegistrationProperties.class)
                            .hasSingleBean(ServiceRegistrationHttpApi.class)
                            .hasSingleBean(ServiceRegistrationClient.class)
                            .hasSingleBean(ServiceRegistrationRunner.class);
                    assertThat(context.getBean(ServiceRegistrationClient.class))
                            .isInstanceOf(HttpServiceRegistrationClient.class);
                    assertThat(context.getBean(
                            GateWeaveServiceRegistrationAutoConfiguration.REGISTRATION_HTTP_CLIENT_BEAN_NAME))
                            .isInstanceOf(HttpClient.class);
                });
    }

    @Test
    void shouldBackOffWhenApplicationProvidesRegistrationClient() {
        ServiceRegistrationClient customClient = metadata -> CatalogRegistrationOutcome.UNCHANGED;

        this.contextRunner.withBean(ServiceRegistrationClient.class, () -> customClient)
                .withPropertyValues("gateweave.registration.base-uri=http://127.0.0.1:8080")
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(ServiceRegistrationClient.class)
                            .hasSingleBean(ServiceRegistrationRunner.class);
                    assertThat(context.getBean(ServiceRegistrationClient.class)).isSameAs(customClient);
                });
    }
}
