package com.lavyoung.gateweave.center.catalog.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.lavyoung.gateweave.center.catalog.application.RegisterServiceDefinitionUseCase;
import com.lavyoung.gateweave.center.catalog.application.DefaultServiceDefinitionRegistrationService;
import com.lavyoung.gateweave.center.catalog.domain.ServiceCatalogRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class CatalogApplicationConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(CatalogApplicationConfiguration.class)
            .withBean(ServiceCatalogRepository.class, () -> mock(ServiceCatalogRepository.class));

    @Test
    void shouldProvideServiceDefinitionRegistrationUseCase() {
        this.contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(RegisterServiceDefinitionUseCase.class);
            assertThat(context.getBean(RegisterServiceDefinitionUseCase.class))
                    .isInstanceOf(DefaultServiceDefinitionRegistrationService.class);
        });
    }
}
