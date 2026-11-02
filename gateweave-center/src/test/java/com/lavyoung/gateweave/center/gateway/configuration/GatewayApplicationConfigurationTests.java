package com.lavyoung.gateweave.center.gateway.configuration;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.lavyoung.gateweave.center.configuration.ControlPlaneClockConfiguration;
import com.lavyoung.gateweave.center.gateway.application.RegisterGatewayNodeUseCase;
import com.lavyoung.gateweave.center.gateway.application.DefaultGatewayNodeRegistrationService;
import com.lavyoung.gateweave.center.gateway.domain.GatewayNode;
import com.lavyoung.gateweave.center.gateway.domain.GatewayNodeRegistration;
import com.lavyoung.gateweave.center.gateway.domain.GatewayNodeRegistrationOutcome;
import com.lavyoung.gateweave.center.gateway.domain.GatewayNodeRegistrationResult;
import com.lavyoung.gateweave.center.gateway.domain.GatewayNodeStatus;
import com.lavyoung.gateweave.center.gateway.domain.GatewayNodeRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GatewayApplicationConfigurationTests {

    @Test
    void shouldAssembleRegistrationUseCaseWithUtcClock() {
        // Given
        GatewayNodeRepository repository = mock(GatewayNodeRepository.class);
        when(repository.registerOrRefresh(any(GatewayNode.class))).thenAnswer(invocation -> {
            GatewayNode node = invocation.getArgument(0);
            return new GatewayNodeRegistrationResult(node, GatewayNodeRegistrationOutcome.REGISTERED);
        });
        ApplicationContextRunner contextRunner = new ApplicationContextRunner()
                .withUserConfiguration(
                        ControlPlaneClockConfiguration.class, GatewayApplicationConfiguration.class)
                .withBean(GatewayNodeRepository.class, () -> repository);

        // When & Then
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(Clock.class);
            assertThat(context).hasSingleBean(RegisterGatewayNodeUseCase.class);
            assertThat(context.getBean(Clock.class).getZone()).isEqualTo(ZoneOffset.UTC);

            RegisterGatewayNodeUseCase useCase = context.getBean(RegisterGatewayNodeUseCase.class);
            assertThat(useCase).isInstanceOf(DefaultGatewayNodeRegistrationService.class);
            GatewayNodeRegistration registration = new GatewayNodeRegistration(
                    "node-1", "default", "Gateway Node 1", URI.create("http://127.0.0.1:8080"));
            Instant beforeRegistration = Instant.now();

            GatewayNodeRegistrationResult result = useCase.register(registration);

            Instant afterRegistration = Instant.now();
            assertThat(result.outcome()).isEqualTo(GatewayNodeRegistrationOutcome.REGISTERED);
            assertThat(result.node().registration()).isEqualTo(registration);
            assertThat(result.node().status()).isEqualTo(GatewayNodeStatus.ONLINE);
            assertThat(result.node().lastHeartbeatAt()).isBetween(beforeRegistration, afterRegistration);
            assertThat(result.node().currentConfigVersion()).isZero();
        });
    }
}
