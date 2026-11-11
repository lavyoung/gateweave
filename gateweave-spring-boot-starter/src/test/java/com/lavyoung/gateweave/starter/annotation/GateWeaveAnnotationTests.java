package com.lavyoung.gateweave.starter.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GateWeaveAnnotationTests {

    @Test
    void shouldExposeServiceMetadataAtRuntime() {
        // When
        GateWeaveService annotation = UserService.class.getAnnotation(GateWeaveService.class);

        // Then
        assertThat(annotation).isNotNull();
        assertThat(annotation.serviceId()).isEqualTo("user-service");
        assertThat(annotation.protocol()).isEqualTo("dubbo");
        assertThat(annotation.namespace()).isEqualTo("production");
        assertThat(annotation.version()).isEqualTo("1.0.0");
    }

    @Test
    void shouldProvideOptionalServiceMetadataDefaults() {
        // When
        GateWeaveService annotation = DefaultService.class.getAnnotation(GateWeaveService.class);

        // Then
        assertThat(annotation).isNotNull();
        assertThat(annotation.protocol()).isEqualTo("dubbo");
        assertThat(annotation.namespace()).isEmpty();
        assertThat(annotation.version()).isEmpty();
    }

    @Test
    void shouldExposeOperationMetadataAtRuntime() throws NoSuchMethodException {
        // Given
        Method method = UserService.class.getDeclaredMethod("findUser", String.class);

        // When
        GateWeaveOperation annotation = method.getAnnotation(GateWeaveOperation.class);

        // Then
        assertThat(annotation).isNotNull();
        assertThat(annotation.operationId()).isEqualTo("find-user");
    }

    @Test
    void shouldDeclareStableAnnotationTargetsAndRetention() {
        // When
        Target serviceTarget = GateWeaveService.class.getAnnotation(Target.class);
        Target operationTarget = GateWeaveOperation.class.getAnnotation(Target.class);
        Retention serviceRetention = GateWeaveService.class.getAnnotation(Retention.class);
        Retention operationRetention = GateWeaveOperation.class.getAnnotation(Retention.class);

        // Then
        assertThat(serviceTarget.value()).containsExactly(ElementType.TYPE);
        assertThat(operationTarget.value()).containsExactly(ElementType.METHOD);
        assertThat(serviceRetention.value()).isEqualTo(RetentionPolicy.RUNTIME);
        assertThat(operationRetention.value()).isEqualTo(RetentionPolicy.RUNTIME);
        assertThat(GateWeaveService.class).hasAnnotation(Documented.class);
        assertThat(GateWeaveOperation.class).hasAnnotation(Documented.class);
    }

    @GateWeaveService(serviceId = "user-service", protocol = "dubbo", namespace = "production", version = "1.0.0")
    private interface UserService {

        @GateWeaveOperation(operationId = "find-user")
        String findUser(String userId);
    }

    @GateWeaveService(serviceId = "default-service")
    private interface DefaultService {
    }
}
