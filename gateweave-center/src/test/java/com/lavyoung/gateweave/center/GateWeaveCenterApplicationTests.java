package com.lavyoung.gateweave.center;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.web.servlet.DispatcherServlet;

import com.lavyoung.gateweave.center.route.adapter.out.persistence.JdbcRouteDefinitionRepository;
import com.lavyoung.gateweave.center.route.application.port.in.QueryRoutePageUseCase;
import com.lavyoung.gateweave.center.route.application.service.DefaultRouteQueryService;
import com.lavyoung.gateweave.center.route.domain.repository.RouteDefinitionRepository;

import static org.assertj.core.api.Assertions.assertThat;

class GateWeaveCenterApplicationTests {

    @Test
    void shouldStartIndependentCenterWebApplicationContext() {
        // Given
        WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
                .withUserConfiguration(GateWeaveCenterApplication.class)
                .withPropertyValues(
                        "spring.datasource.url=jdbc:h2:mem:gateweave-center;MODE=MySQL;DB_CLOSE_DELAY=0",
                        "spring.datasource.username=sa",
                        "spring.datasource.password=");

        // When & Then
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(GateWeaveCenterApplication.class);
            assertThat(context).hasSingleBean(DispatcherServlet.class);
            assertThat(context).hasSingleBean(DataSource.class);
            assertThat(context).hasSingleBean(Flyway.class);
            assertThat(context).hasSingleBean(RouteDefinitionRepository.class);
            assertThat(context).hasSingleBean(JdbcRouteDefinitionRepository.class);
            assertThat(context).hasSingleBean(QueryRoutePageUseCase.class);
            assertThat(context).hasSingleBean(DefaultRouteQueryService.class);
        });
    }

    @Test
    void shouldUseRootPackageScanningWithoutConfigurationProxy() {
        // Given
        SpringBootApplication annotation = GateWeaveCenterApplication.class.getAnnotation(SpringBootApplication.class);

        // When & Then
        assertThat(annotation).isNotNull();
        assertThat(annotation.proxyBeanMethods()).isFalse();
        assertThat(annotation.scanBasePackages()).isEmpty();
        assertThat(annotation.scanBasePackageClasses()).isEmpty();
    }
}
