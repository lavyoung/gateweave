package com.lavyoung.gateweave.plugin.invoker.rpc;

import java.util.concurrent.CompletableFuture;

import org.apache.dubbo.config.ReferenceConfigBase;
import org.apache.dubbo.config.utils.SimpleReferenceCache;
import org.apache.dubbo.rpc.service.GenericService;
import org.junit.jupiter.api.Test;

import com.lavyoung.gateweave.contract.extension.IGenericReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GenericReferenceRegistryTests {

    @Test
    void shouldRegisterAndInvokeGenericReference() {
        // Given
        var referenceCache = mock(SimpleReferenceCache.class);
        var genericService = mock(GenericService.class);
        when(referenceCache.<GenericService>get(any(ReferenceConfigBase.class))).thenReturn(genericService);
        when(genericService.$invokeAsync(eq("sayHi"), aryEq(new String[]{"java.lang.String"}),
                aryEq(new Object[]{"gateweave"})))
                .thenReturn(CompletableFuture.completedFuture("hi gateweave"));

        try (var cacheFactory = mockStatic(SimpleReferenceCache.class)) {
            cacheFactory.when(SimpleReferenceCache::newCache).thenReturn(referenceCache);
            try (var registry = new GenericReferenceRegistry("gateweave", "zookeeper://127.0.0.1:2181")) {
                registry.addGenericReference("cn.example.ActivityBooth", "sayHi", "1.0.0");

                // When
                IGenericReference reference = registry.getGenericReference("sayHi");
                String result = reference.$invoke("gateweave").toCompletableFuture().join();

                // Then
                assertThat(result).isEqualTo("hi gateweave");
                verify(referenceCache).get(any(ReferenceConfigBase.class));
            }
        }

        verify(referenceCache).destroyAll();
    }

    @Test
    void shouldReuseRegistrationForSameMethod() {
        // Given
        var referenceCache = mock(SimpleReferenceCache.class);
        when(referenceCache.<GenericService>get(any(ReferenceConfigBase.class))).thenReturn(mock(GenericService.class));

        try (var cacheFactory = mockStatic(SimpleReferenceCache.class)) {
            cacheFactory.when(SimpleReferenceCache::newCache).thenReturn(referenceCache);
            try (var registry = new GenericReferenceRegistry("gateweave", "zookeeper://127.0.0.1:2181")) {
                // When
                registry.addGenericReference("cn.example.ActivityBooth", "sayHi", "1.0.0");
                registry.addGenericReference("cn.example.ActivityBooth", "sayHi", "1.0.0");

                // Then
                verify(referenceCache, times(1)).get(any(ReferenceConfigBase.class));
            }
        }
    }

    @Test
    void shouldRejectLookupOfUnregisteredMethod() {
        // Given
        var referenceCache = mock(SimpleReferenceCache.class);

        try (var cacheFactory = mockStatic(SimpleReferenceCache.class)) {
            cacheFactory.when(SimpleReferenceCache::newCache).thenReturn(referenceCache);
            try (var registry = new GenericReferenceRegistry("gateweave", "zookeeper://127.0.0.1:2181")) {
                // When & Then
                assertThatThrownBy(() -> registry.getGenericReference("unknown"))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage("generic reference is not registered: unknown");
            }
        }
    }

    @Test
    void shouldRemoveReferencesAndDestroyCacheWhenClosed() {
        // Given
        var referenceCache = mock(SimpleReferenceCache.class);
        when(referenceCache.<GenericService>get(any(ReferenceConfigBase.class))).thenReturn(mock(GenericService.class));

        try (var cacheFactory = mockStatic(SimpleReferenceCache.class)) {
            cacheFactory.when(SimpleReferenceCache::newCache).thenReturn(referenceCache);
            var registry = new GenericReferenceRegistry("gateweave", "zookeeper://127.0.0.1:2181");
            registry.addGenericReference("cn.example.ActivityBooth", "sayHi", "1.0.0");

            // When
            registry.close();

            // Then
            verify(referenceCache).destroyAll();
            assertThatThrownBy(() -> registry.getGenericReference("sayHi"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("generic reference is not registered: sayHi");
        }
    }
}
