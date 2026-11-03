package com.lavyoung.gateweave.contract.extension;

import com.lavyoung.gateweave.contract.configuration.InvocationTargetConfiguration;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProtocolInvokerTests {

    @Test
    void shouldUseDefaultPreparationForProtocolWithoutRuntimeResources() {
        // Given
        ProtocolInvoker invoker = statelessInvoker();
        InvocationTargetConfiguration target = target();

        // When & Then
        assertThatCode(() -> invoker.prepare(target)).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectNullTargetDuringDefaultPreparation() {
        // Given
        ProtocolInvoker invoker = statelessInvoker();

        // When & Then
        assertThatThrownBy(() -> invoker.prepare(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("target must not be null");
    }

    private static ProtocolInvoker statelessInvoker() {
        return new ProtocolInvoker() {
            @Override
            public String protocol() {
                return "stateless";
            }

            @Override
            public CompletionStage<String> invoke(InvocationRequest request) {
                return CompletableFuture.completedFuture("ok");
            }
        };
    }

    private static InvocationTargetConfiguration target() {
        return new InvocationTargetConfiguration(
                "stateless", "service", "operation", List.of(), Duration.ofSeconds(1), Map.of());
    }
}
