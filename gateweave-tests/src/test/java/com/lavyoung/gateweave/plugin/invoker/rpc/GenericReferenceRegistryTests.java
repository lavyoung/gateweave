package com.lavyoung.gateweave.plugin.invoker.rpc;

import java.util.concurrent.CompletableFuture;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.config.ReferenceConfigBase;
import org.apache.dubbo.config.utils.SimpleReferenceCache;
import org.apache.dubbo.rpc.service.GenericService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.lavyoung.gateweave.contract.extension.IGenericReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GenericReferenceRegistryTests {

    @Test
    void shouldMapDiscoveryModesToDubboSettings() {
        // When & Then
        assertThat(DubboDiscoveryMode.INTERFACE.migrationStep()).isEqualTo("FORCE_INTERFACE");
        assertThat(DubboDiscoveryMode.INTERFACE.registerMode()).isEqualTo(CommonConstants.INTERFACE_REGISTER_MODE);
        assertThat(DubboDiscoveryMode.APPLICATION_FIRST.migrationStep()).isEqualTo("APPLICATION_FIRST");
        assertThat(DubboDiscoveryMode.APPLICATION_FIRST.registerMode())
                .isEqualTo(CommonConstants.DEFAULT_REGISTER_MODE);
        assertThat(DubboDiscoveryMode.APPLICATION.migrationStep()).isEqualTo("FORCE_APPLICATION");
        assertThat(DubboDiscoveryMode.APPLICATION.registerMode()).isEqualTo(CommonConstants.INSTANCE_REGISTER_MODE);
    }

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
            try (var registry = new GenericReferenceRegistry(
                    "gateweave", "zookeeper://127.0.0.1:2181", DubboDiscoveryMode.INTERFACE)) {
                registry.addGenericReference("cn.example.ActivityBooth", "sayHi", "1.0.0");

                // When
                IGenericReference reference = registry.getGenericReference(
                        "cn.example.ActivityBooth", "sayHi", "1.0.0");
                String result = reference.$invoke("gateweave").toCompletableFuture().join();

                // Then
                assertThat(result).isEqualTo("hi gateweave");
                @SuppressWarnings({"rawtypes", "unchecked"})
                ArgumentCaptor<ReferenceConfigBase<GenericService>> referenceCaptor =
                        ArgumentCaptor.forClass(ReferenceConfigBase.class);
                verify(referenceCache).get(referenceCaptor.capture());
                assertThat(referenceCaptor.getValue().getParameters())
                        .containsEntry("migration.step", "FORCE_INTERFACE");
                assertThat(referenceCaptor.getValue().getApplication().getQosEnable()).isFalse();
                assertThat(referenceCaptor.getValue().getApplication().getRegisterMode())
                        .isEqualTo(CommonConstants.INTERFACE_REGISTER_MODE);
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
            try (var registry = new GenericReferenceRegistry(
                    "gateweave", "zookeeper://127.0.0.1:2181", DubboDiscoveryMode.INTERFACE)) {
                // When
                registry.addGenericReference("cn.example.ActivityBooth", "sayHi", "1.0.0");
                registry.addGenericReference("cn.example.ActivityBooth", "sayHi", "1.0.0");

                // Then
                verify(referenceCache, times(1)).get(any(ReferenceConfigBase.class));
            }
        }
    }

    @Test
    void shouldIsolateSameMethodNameAcrossInterfaces() {
        // Given
        var referenceCache = mock(SimpleReferenceCache.class);
        var firstService = mock(GenericService.class);
        var secondService = mock(GenericService.class);
        when(referenceCache.<GenericService>get(any(ReferenceConfigBase.class)))
                .thenReturn(firstService, secondService);
        when(firstService.$invokeAsync(eq("sayHi"), any(String[].class), any(Object[].class)))
                .thenReturn(CompletableFuture.completedFuture("first"));
        when(secondService.$invokeAsync(eq("sayHi"), any(String[].class), any(Object[].class)))
                .thenReturn(CompletableFuture.completedFuture("second"));

        try (var cacheFactory = mockStatic(SimpleReferenceCache.class)) {
            cacheFactory.when(SimpleReferenceCache::newCache).thenReturn(referenceCache);
            try (var registry = new GenericReferenceRegistry(
                    "gateweave", "zookeeper://127.0.0.1:2181", DubboDiscoveryMode.INTERFACE)) {
                registry.addGenericReference("cn.example.FirstService", "sayHi", "1.0.0");
                registry.addGenericReference("cn.example.SecondService", "sayHi", "1.0.0");

                // When
                var firstResult = registry.getGenericReference("cn.example.FirstService", "sayHi", "1.0.0")
                        .$invoke("gateweave").toCompletableFuture().join();
                var secondResult = registry.getGenericReference("cn.example.SecondService", "sayHi", "1.0.0")
                        .$invoke("gateweave").toCompletableFuture().join();

                // Then
                assertThat(firstResult).isEqualTo("first");
                assertThat(secondResult).isEqualTo("second");
                verify(referenceCache, times(2)).get(any(ReferenceConfigBase.class));
            }
        }
    }

    @Test
    void shouldRejectLookupOfUnregisteredMethod() {
        // Given
        var referenceCache = mock(SimpleReferenceCache.class);

        try (var cacheFactory = mockStatic(SimpleReferenceCache.class)) {
            cacheFactory.when(SimpleReferenceCache::newCache).thenReturn(referenceCache);
            try (var registry = new GenericReferenceRegistry(
                    "gateweave", "zookeeper://127.0.0.1:2181", DubboDiscoveryMode.INTERFACE)) {
                // When & Then
                assertThatThrownBy(() -> registry.getGenericReference(
                        "cn.example.ActivityBooth", "unknown", "1.0.0"))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage("generic reference is not registered: cn.example.ActivityBooth#unknown:1.0.0");
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
            var registry = new GenericReferenceRegistry(
                    "gateweave", "zookeeper://127.0.0.1:2181", DubboDiscoveryMode.INTERFACE);
            registry.addGenericReference("cn.example.ActivityBooth", "sayHi", "1.0.0");

            // When
            registry.close();
            registry.close();

            // Then
            verify(referenceCache, times(1)).destroyAll();
            assertThatThrownBy(() -> registry.getGenericReference(
                    "cn.example.ActivityBooth", "sayHi", "1.0.0"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("generic reference is not registered: cn.example.ActivityBooth#sayHi:1.0.0");
        }
    }

    @Test
    void shouldAllowRetryWhenClosingReferencesFails() {
        // Given
        var referenceCache = mock(SimpleReferenceCache.class);
        doThrow(new IllegalStateException("close failed")).doNothing().when(referenceCache).destroyAll();

        try (var cacheFactory = mockStatic(SimpleReferenceCache.class)) {
            cacheFactory.when(SimpleReferenceCache::newCache).thenReturn(referenceCache);
            var registry = new GenericReferenceRegistry(
                    "gateweave", "zookeeper://127.0.0.1:2181", DubboDiscoveryMode.INTERFACE);

            // When & Then
            assertThatThrownBy(registry::close)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("close failed");
            registry.close();
            registry.close();
            verify(referenceCache, times(2)).destroyAll();
        }
    }
}
