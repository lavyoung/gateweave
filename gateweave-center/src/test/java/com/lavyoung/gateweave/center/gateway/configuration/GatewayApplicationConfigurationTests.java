package com.lavyoung.gateweave.center.gateway.configuration;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import com.lavyoung.gateweave.center.configuration.ControlPlaneClockConfiguration;
import com.lavyoung.gateweave.center.gateway.application.ExpireGatewayNodeLeasesUseCase;
import com.lavyoung.gateweave.center.gateway.application.RegisterGatewayNodeUseCase;
import com.lavyoung.gateweave.center.gateway.application.ReleaseGatewayNodeLeaseUseCase;
import com.lavyoung.gateweave.center.gateway.application.RenewGatewayNodeLeaseUseCase;
import com.lavyoung.gateweave.center.gateway.application.service.DefaultGatewayNodeLeaseService;
import com.lavyoung.gateweave.center.gateway.application.service.DefaultGatewayNodeRegistrationService;
import com.lavyoung.gateweave.center.gateway.domain.GatewayNode;
import com.lavyoung.gateweave.center.gateway.domain.GatewayNodeRegistration;
import com.lavyoung.gateweave.center.gateway.domain.GatewayNodeRegistrationOutcome;
import com.lavyoung.gateweave.center.gateway.domain.GatewayNodeRegistrationResult;
import com.lavyoung.gateweave.center.gateway.domain.GatewayNodeStatus;
import com.lavyoung.gateweave.center.gateway.domain.GatewayNodeRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GatewayApplicationConfigurationTests {

    @Test
    void shouldAssembleGatewayUseCasesWithUtcClock() {
        // Given
        GatewayNodeRepository repository = mock(GatewayNodeRepository.class);
        when(repository.registerOrRefresh(any(GatewayNode.class))).thenAnswer(invocation -> {
            GatewayNode node = invocation.getArgument(0);
            return new GatewayNodeRegistrationResult(node, GatewayNodeRegistrationOutcome.REGISTERED);
        });
        when(repository.renewLease(any())).thenReturn(true);
        when(repository.markExpiredLeasesOffline(any())).thenReturn(2);
        AtomicReference<ThreadPoolTaskScheduler> taskSchedulerReference = new AtomicReference<>();
        ApplicationContextRunner contextRunner =
                new ApplicationContextRunner().withUserConfiguration(ControlPlaneClockConfiguration.class,
                                GatewayApplicationConfiguration.class).withBean(GatewayNodeRepository.class,
                                () -> repository)
                        .withPropertyValues("gateweave.gateway-node.lease.heartbeat-interval=15s",
                                "gateweave.gateway-node.lease.lease-timeout=45s",
                                "gateweave.gateway-node.lease.expiration-scan-interval=1h");

        // When & Then
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(Clock.class);
            assertThat(context).hasSingleBean(RegisterGatewayNodeUseCase.class);
            assertThat(context).hasSingleBean(RenewGatewayNodeLeaseUseCase.class);
            assertThat(context).hasSingleBean(ExpireGatewayNodeLeasesUseCase.class);
            assertThat(context).hasSingleBean(ReleaseGatewayNodeLeaseUseCase.class);
            assertThat(context).hasSingleBean(DefaultGatewayNodeLeaseService.class);
            assertThat(context).hasSingleBean(GatewayNodeLeaseProperties.class);
            assertThat(context).hasSingleBean(GatewayNodeLeaseExpirationScheduler.class);
            assertThat(context).hasSingleBean(ThreadPoolTaskScheduler.class);
            assertThat(context.getBean(Clock.class).getZone()).isEqualTo(ZoneOffset.UTC);
            GatewayNodeLeaseProperties leaseProperties = context.getBean(GatewayNodeLeaseProperties.class);
            assertThat(leaseProperties.heartbeatInterval()).isEqualTo(Duration.ofSeconds(15));
            assertThat(leaseProperties.leaseTimeout()).isEqualTo(Duration.ofSeconds(45));
            assertThat(leaseProperties.expirationScanInterval()).isEqualTo(Duration.ofHours(1));
            ThreadPoolTaskScheduler taskScheduler = context.getBean(ThreadPoolTaskScheduler.class);
            taskSchedulerReference.set(taskScheduler);
            assertThat(taskScheduler.getPoolSize()).isEqualTo(1);
            assertThat(taskScheduler.getThreadNamePrefix()).isEqualTo("gateweave-node-maintenance-");

            RegisterGatewayNodeUseCase useCase = context.getBean(RegisterGatewayNodeUseCase.class);
            assertThat(useCase).isInstanceOf(DefaultGatewayNodeRegistrationService.class);
            GatewayNodeRegistration registration = new GatewayNodeRegistration("node-1", "default", "Gateway Node 1",
                    URI.create("http://127.0.0.1:8080"));
            Instant beforeRegistration = Instant.now();

            GatewayNodeRegistrationResult result = useCase.register(registration);

            Instant afterRegistration = Instant.now();
            assertThat(result.outcome()).isEqualTo(GatewayNodeRegistrationOutcome.REGISTERED);
            assertThat(result.node().registration()).isEqualTo(registration);
            assertThat(result.node().status()).isEqualTo(GatewayNodeStatus.ONLINE);
            assertThat(result.node().lastHeartbeatAt()).isBetween(beforeRegistration, afterRegistration);
            assertThat(result.node().currentConfigVersion()).isZero();

            RenewGatewayNodeLeaseUseCase leaseUseCase = context.getBean(RenewGatewayNodeLeaseUseCase.class);
            ExpireGatewayNodeLeasesUseCase expirationUseCase = context.getBean(ExpireGatewayNodeLeasesUseCase.class);
            ReleaseGatewayNodeLeaseUseCase releaseUseCase = context.getBean(ReleaseGatewayNodeLeaseUseCase.class);
            assertThat(leaseUseCase).isInstanceOf(DefaultGatewayNodeLeaseService.class);
            assertThat(expirationUseCase).isSameAs(leaseUseCase);
            assertThat(releaseUseCase).isSameAs(leaseUseCase);
            leaseUseCase.renew("node-1", UUID.fromString("00000000-0000-0000-0000-000000000001"), 0);
            verify(repository).renewLease(any());
            assertThat(expirationUseCase.expire()).isEqualTo(2);
            verify(repository).markExpiredLeasesOffline(any());
        });

        assertThat(taskSchedulerReference.get().getScheduledThreadPoolExecutor().isShutdown()).isTrue();
        assertThat(taskSchedulerReference.get().getScheduledThreadPoolExecutor().isTerminated()).isTrue();
    }
}
