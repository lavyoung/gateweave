package com.lavyoung.gateweave.plugin.invoker.rpc;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import org.apache.dubbo.rpc.service.GenericService;
import org.junit.jupiter.api.Test;

import com.lavyoung.gateweave.contract.extension.IGenericReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class GenericReferenceProxyFactoryTests {

    @Test
    void shouldReuseProxyForSameMethod() {
        // Given
        var factory = new GenericReferenceProxyFactory(mock(GenericService.class));

        // When
        var firstReference = factory.newInstance("sayHi");
        var secondReference = factory.newInstance("sayHi");

        // Then
        assertThat(firstReference).isSameAs(secondReference);
    }

    @Test
    void shouldCreateDifferentProxiesForDifferentMethods() {
        // Given
        var factory = new GenericReferenceProxyFactory(mock(GenericService.class));

        // When
        var sayHiReference = factory.newInstance("sayHi");
        var goodbyeReference = factory.newInstance("goodbye");

        // Then
        assertThat(sayHiReference).isNotSameAs(goodbyeReference);
    }

    @Test
    void shouldReuseSingleProxyDuringConcurrentAccess() {
        // Given
        var factory = new GenericReferenceProxyFactory(mock(GenericService.class));
        var executor = Executors.newFixedThreadPool(8);

        try {
            // When
            var references = IntStream.range(0, 32)
                    .mapToObj(index -> CompletableFuture.supplyAsync(() -> factory.newInstance("sayHi"), executor))
                    .toList();
            IGenericReference expectedReference = references.get(0).join();

            // Then
            references.forEach(reference -> assertThat(reference.join()).isSameAs(expectedReference));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void shouldRejectInvalidFactoryArguments() {
        // Given
        var factory = new GenericReferenceProxyFactory(mock(GenericService.class));

        // When & Then
        assertThatThrownBy(() -> new GenericReferenceProxyFactory(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("genericService must not be null");
        assertThatThrownBy(() -> factory.newInstance(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("methodName must not be null");
        assertThatThrownBy(() -> factory.newInstance(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("methodName must not be blank");
    }
}
