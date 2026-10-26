package com.lavyoung.gateweave.plugin.invoker.rpc;

import java.lang.reflect.Proxy;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.apache.dubbo.rpc.service.GenericService;
import org.junit.jupiter.api.Test;

import com.lavyoung.gateweave.contract.extension.IGenericReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GenericReferenceProxyTests {

    @Test
    void shouldConvertGenericReferenceInvocationToDubboInvocation() {
        // Given
        var genericService = mock(GenericService.class);
        when(genericService.$invokeAsync(eq("sayHi"), aryEq(new String[]{"java.lang.String"}),
                aryEq(new Object[]{"gateweave"})))
                .thenReturn(CompletableFuture.completedFuture("hi gateweave"));
        var reference = createReference(genericService, "sayHi");

        // When
        String result = reference.$invoke("gateweave").toCompletableFuture().join();

        // Then
        assertThat(result).isEqualTo("hi gateweave");
        verify(genericService).$invokeAsync(eq("sayHi"), aryEq(new String[]{"java.lang.String"}),
                aryEq(new Object[]{"gateweave"}));
    }

    @Test
    void shouldRejectInvalidMethodName() {
        // Given
        var genericService = mock(GenericService.class);

        // When & Then
        assertThatThrownBy(() -> new GenericReferenceProxy(null, "sayHi"))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("genericService must not be null");
        assertThatThrownBy(() -> new GenericReferenceProxy(genericService, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("methodName must not be null");
        assertThatThrownBy(() -> new GenericReferenceProxy(genericService, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("methodName must not be blank");
    }

    @Test
    void shouldRejectNonStringInvocationResult() {
        // Given
        var genericService = mock(GenericService.class);
        when(genericService.$invokeAsync(eq("sayHi"), aryEq(new String[]{"java.lang.String"}),
                aryEq(new Object[]{"gateweave"})))
                .thenReturn(CompletableFuture.completedFuture(1));
        var reference = createReference(genericService, "sayHi");

        // When & Then
        assertThatThrownBy(() -> reference.$invoke("gateweave").toCompletableFuture().join())
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("generic invocation result must be a String");
    }

    @Test
    void shouldPropagateAsynchronousInvocationFailure() {
        // Given
        var genericService = mock(GenericService.class);
        var failure = new IllegalStateException("rpc unavailable");
        when(genericService.$invokeAsync(eq("sayHi"), aryEq(new String[]{"java.lang.String"}),
                aryEq(new Object[]{"gateweave"})))
                .thenReturn(CompletableFuture.failedFuture(failure));
        var reference = createReference(genericService, "sayHi");

        // When & Then
        assertThatThrownBy(() -> reference.$invoke("gateweave").toCompletableFuture().join())
                .isInstanceOf(CompletionException.class)
                .hasCause(failure);
    }

    private static IGenericReference createReference(GenericService genericService, String methodName) {
        var invocationHandler = new GenericReferenceProxy(genericService, methodName);
        return (IGenericReference) Proxy.newProxyInstance(IGenericReference.class.getClassLoader(),
                new Class<?>[]{IGenericReference.class}, invocationHandler);
    }
}
