package com.lavyoung.gateweave.plugin.invoker.rpc;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

import org.junit.jupiter.api.Test;

import com.lavyoung.gateweave.contract.configuration.InvocationTargetConfiguration;
import com.lavyoung.gateweave.contract.extension.GenericInvocationArguments;
import com.lavyoung.gateweave.contract.extension.IGenericReference;
import com.lavyoung.gateweave.contract.extension.InvocationRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DubboProtocolInvokerTests {

    @Test
    void shouldExposeDubboProtocol() {
        // Given
        GenericReferenceRegistry registry = mock(GenericReferenceRegistry.class);
        DubboProtocolInvoker invoker = new DubboProtocolInvoker(registry);

        // When & Then
        assertThat(invoker.protocol()).isEqualTo("dubbo");
    }

    @Test
    void shouldPrepareGenericReferenceFromTargetConfiguration() {
        // Given
        GenericReferenceRegistry registry = mock(GenericReferenceRegistry.class);
        InvocationTargetConfiguration target = target(Map.of("version", "1.0.0"));
        DubboProtocolInvoker invoker = new DubboProtocolInvoker(registry);

        // When
        invoker.prepare(target);

        // Then
        verify(registry).addGenericReference("demo.GreetingService", "sayHello", "1.0.0");
    }

    @Test
    void shouldRejectMissingVersionBeforePreparingReference() {
        // Given
        GenericReferenceRegistry registry = mock(GenericReferenceRegistry.class);
        InvocationTargetConfiguration target = target(Map.of());
        DubboProtocolInvoker invoker = new DubboProtocolInvoker(registry);

        // When & Then
        assertThatThrownBy(() -> invoker.prepare(target))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("version must not be null");
        verifyNoInteractions(registry);
    }

    @Test
    void shouldRejectNullTargetBeforePreparingReference() {
        // Given
        GenericReferenceRegistry registry = mock(GenericReferenceRegistry.class);
        DubboProtocolInvoker invoker = new DubboProtocolInvoker(registry);

        // When & Then
        assertThatThrownBy(() -> invoker.prepare(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("target must not be null");
        verifyNoInteractions(registry);
    }

    @Test
    void shouldPassResolvedArgumentsToGenericReference() {
        // Given
        GenericReferenceRegistry registry = mock(GenericReferenceRegistry.class);
        IGenericReference reference = mock(IGenericReference.class);
        GenericInvocationArguments arguments = stringArguments("你好，GateWeave");
        InvocationRequest request = request(Map.of("version", "1.0.0"), arguments);
        when(registry.getGenericReference("demo.GreetingService", "sayHello", "1.0.0")).thenReturn(reference);
        when(reference.$invoke(arguments)).thenReturn(CompletableFuture.completedFuture("调用成功"));
        DubboProtocolInvoker invoker = new DubboProtocolInvoker(registry);

        // When
        String result = invoker.invoke(request).toCompletableFuture().join();

        // Then
        assertThat(result).isEqualTo("调用成功");
        verify(registry).getGenericReference("demo.GreetingService", "sayHello", "1.0.0");
        verify(reference).$invoke(arguments);
    }

    @Test
    void shouldPreserveAsynchronousInvocationFailure() {
        // Given
        GenericReferenceRegistry registry = mock(GenericReferenceRegistry.class);
        IGenericReference reference = mock(IGenericReference.class);
        GenericInvocationArguments arguments = stringArguments("GateWeave");
        InvocationRequest request = request(Map.of("version", "1.0.0"), arguments);
        IllegalStateException failure = new IllegalStateException("RPC unavailable");
        when(registry.getGenericReference("demo.GreetingService", "sayHello", "1.0.0")).thenReturn(reference);
        when(reference.$invoke(arguments)).thenReturn(CompletableFuture.failedFuture(failure));
        DubboProtocolInvoker invoker = new DubboProtocolInvoker(registry);

        // When
        CompletionStage<String> resultStage = invoker.invoke(request);

        // Then
        assertThatThrownBy(() -> resultStage.toCompletableFuture().join())
                .isInstanceOf(CompletionException.class)
                .hasCause(failure);
    }

    @Test
    void shouldRejectMissingVersionBeforeResolvingReference() {
        // Given
        GenericReferenceRegistry registry = mock(GenericReferenceRegistry.class);
        InvocationRequest request = request(Map.of(), stringArguments("GateWeave"));
        DubboProtocolInvoker invoker = new DubboProtocolInvoker(registry);

        // When & Then
        assertThatThrownBy(() -> invoker.invoke(request))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("version must not be null");
        verifyNoInteractions(registry);
    }

    @Test
    void shouldRejectNullStageReturnedByGenericReference() {
        // Given
        GenericReferenceRegistry registry = mock(GenericReferenceRegistry.class);
        IGenericReference reference = mock(IGenericReference.class);
        GenericInvocationArguments arguments = stringArguments("GateWeave");
        InvocationRequest request = request(Map.of("version", "1.0.0"), arguments);
        when(registry.getGenericReference("demo.GreetingService", "sayHello", "1.0.0")).thenReturn(reference);
        when(reference.$invoke(arguments)).thenReturn(null);
        DubboProtocolInvoker invoker = new DubboProtocolInvoker(registry);

        // When & Then
        assertThatThrownBy(() -> invoker.invoke(request))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("generic reference must not return null");
    }

    @Test
    void shouldRejectMissingRegistryOrRequest() {
        // Given
        GenericReferenceRegistry registry = mock(GenericReferenceRegistry.class);
        DubboProtocolInvoker invoker = new DubboProtocolInvoker(registry);

        // When & Then
        assertThatThrownBy(() -> new DubboProtocolInvoker(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("referenceRegistry must not be null");
        assertThatThrownBy(() -> invoker.invoke(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("request must not be null");
    }

    private static InvocationRequest request(
            Map<String, String> attributes, GenericInvocationArguments invocationArguments) {
        return new InvocationRequest("request-1", "route-1", "dubbo", "demo.GreetingService", "sayHello",
                invocationArguments, Duration.ofSeconds(3), Map.of(), attributes, new byte[]{1, 2, 3});
    }

    private static GenericInvocationArguments stringArguments(String value) {
        return new GenericInvocationArguments(List.of("java.lang.String"), List.of(value));
    }

    private static InvocationTargetConfiguration target(Map<String, String> attributes) {
        return new InvocationTargetConfiguration("dubbo", "demo.GreetingService", "sayHello", List.of(),
                Duration.ofSeconds(3), attributes);
    }
}
