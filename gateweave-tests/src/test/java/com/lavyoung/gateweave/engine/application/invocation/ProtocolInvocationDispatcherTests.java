package com.lavyoung.gateweave.engine.application.invocation;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.junit.jupiter.api.Test;

import com.lavyoung.gateweave.contract.extension.InvocationRequest;
import com.lavyoung.gateweave.contract.extension.ProtocolInvoker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProtocolInvocationDispatcherTests {

    @Test
    void shouldDispatchRequestByNormalizedProtocol() {
        // Given
        InvocationRequest request = request("DUBBO");
        ProtocolInvoker invoker = mock(ProtocolInvoker.class);
        when(invoker.protocol()).thenReturn("DuBbO");
        when(invoker.invoke(request)).thenReturn(CompletableFuture.completedFuture("Hello GateWeave"));
        ProtocolInvocationDispatcher dispatcher = new ProtocolInvocationDispatcher(List.of(invoker));

        // When
        String result = dispatcher.invoke(request).toCompletableFuture().join();

        // Then
        assertThat(result).isEqualTo("Hello GateWeave");
        verify(invoker).invoke(request);
    }

    @Test
    void shouldRejectDuplicateProtocolsIgnoringCase() {
        // Given
        ProtocolInvoker first = mock(ProtocolInvoker.class);
        ProtocolInvoker second = mock(ProtocolInvoker.class);
        when(first.protocol()).thenReturn("dubbo");
        when(second.protocol()).thenReturn("DUBBO");

        // When & Then
        assertThatThrownBy(() -> new ProtocolInvocationDispatcher(List.of(first, second)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("duplicate protocol invoker: dubbo");
    }

    @Test
    void shouldReturnFailedStageWhenProtocolIsNotRegistered() {
        // Given
        ProtocolInvoker invoker = mock(ProtocolInvoker.class);
        when(invoker.protocol()).thenReturn("dubbo");
        ProtocolInvocationDispatcher dispatcher = new ProtocolInvocationDispatcher(List.of(invoker));

        // When & Then
        assertThatThrownBy(() -> dispatcher.invoke(request("http")).toCompletableFuture().join())
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("No protocol invoker registered for: http");
    }

    @Test
    void shouldConvertSynchronousInvocationFailureToFailedStage() {
        // Given
        InvocationRequest request = request("dubbo");
        IllegalStateException failure = new IllegalStateException("RPC unavailable");
        ProtocolInvoker invoker = mock(ProtocolInvoker.class);
        when(invoker.protocol()).thenReturn("dubbo");
        when(invoker.invoke(request)).thenThrow(failure);
        ProtocolInvocationDispatcher dispatcher = new ProtocolInvocationDispatcher(List.of(invoker));

        // When & Then
        assertThatThrownBy(() -> dispatcher.invoke(request).toCompletableFuture().join())
                .isInstanceOf(CompletionException.class)
                .hasCause(failure);
    }

    @Test
    void shouldReturnFailedStageWhenInvokerReturnsNull() {
        // Given
        InvocationRequest request = request("dubbo");
        ProtocolInvoker invoker = mock(ProtocolInvoker.class);
        when(invoker.protocol()).thenReturn("dubbo");
        when(invoker.invoke(request)).thenReturn(null);
        ProtocolInvocationDispatcher dispatcher = new ProtocolInvocationDispatcher(List.of(invoker));

        // When & Then
        assertThatThrownBy(() -> dispatcher.invoke(request).toCompletableFuture().join())
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(NullPointerException.class)
                .hasRootCauseMessage("protocol invoker must not return null");
    }

    @Test
    void shouldRejectInvalidConstructorArguments() {
        // Given
        ProtocolInvoker nullProtocolInvoker = mock(ProtocolInvoker.class);
        ProtocolInvoker blankProtocolInvoker = mock(ProtocolInvoker.class);
        when(nullProtocolInvoker.protocol()).thenReturn(null);
        when(blankProtocolInvoker.protocol()).thenReturn(" ");

        // When & Then
        assertThatThrownBy(() -> new ProtocolInvocationDispatcher(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("invokers must not be null");
        assertThatThrownBy(() -> new ProtocolInvocationDispatcher(List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("invokers must not be empty");
        assertThatThrownBy(() -> new ProtocolInvocationDispatcher(List.of(nullProtocolInvoker)))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("protocol invoker must return a protocol");
        assertThatThrownBy(() -> new ProtocolInvocationDispatcher(List.of(blankProtocolInvoker)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("protocol invoker protocol must not be blank");
    }

    @Test
    void shouldRejectMissingRequest() {
        // Given
        ProtocolInvoker invoker = mock(ProtocolInvoker.class);
        when(invoker.protocol()).thenReturn("dubbo");
        ProtocolInvocationDispatcher dispatcher = new ProtocolInvocationDispatcher(List.of(invoker));

        // When & Then
        assertThatThrownBy(() -> dispatcher.invoke(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("request must not be null");
    }

    private static InvocationRequest request(String protocol) {
        return new InvocationRequest("request-1", "route-1", protocol, "demo.GreetingService", "sayHello",
                List.of("java.lang.String"), Duration.ofSeconds(3), Map.of(), Map.of(),
                "GateWeave".getBytes(StandardCharsets.UTF_8));
    }
}
