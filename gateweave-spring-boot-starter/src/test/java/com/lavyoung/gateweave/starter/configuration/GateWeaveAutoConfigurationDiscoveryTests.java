package com.lavyoung.gateweave.starter.configuration;

import com.lavyoung.gateweave.starter.discovery.ServiceMetadataBeanPostProcessor;
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
            assertThat(context).hasSingleBean(ServiceMetadataBeanPostProcessor.class);
            assertThat(context)
                    .hasBean(GateWeaveServiceMetadataAutoConfiguration.SERVICE_METADATA_CONSUMER_BEAN_NAME);
        });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class TestApplication {
    }
}
