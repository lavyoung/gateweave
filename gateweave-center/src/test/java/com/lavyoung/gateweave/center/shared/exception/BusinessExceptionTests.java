package com.lavyoung.gateweave.center.shared.exception;

import com.lavyoung.gateweave.contract.error.ResponseCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BusinessExceptionTests {

    @Test
    void shouldPreserveResponseCodeAndFormatDefaultMessage() {
        // Given & When
        BusinessException exception = new BusinessException(TestResponseCode.RESOURCE_NOT_FOUND, "gateway-group-1");

        // Then
        assertThat(exception.responseCode()).isSameAs(TestResponseCode.RESOURCE_NOT_FOUND);
        assertThat(exception.arguments()).containsExactly("gateway-group-1");
        assertThat(exception).hasMessage("Resource gateway-group-1 does not exist");
    }

    @Test
    void shouldPreserveOriginalCause() {
        // Given
        IllegalStateException cause = new IllegalStateException("database unavailable");

        // When
        BusinessException exception = new BusinessException(TestResponseCode.OPERATION_FAILED, cause);

        // Then
        assertThat(exception).hasCauseReference(cause);
        assertThat(exception).hasMessage("Operation failed");
        assertThat(exception.arguments()).isEmpty();
    }

    @Test
    void shouldDefensivelyCopyMessageArguments() {
        // Given
        Object[] arguments = {"gateway-group-1"};
        BusinessException exception = new BusinessException(TestResponseCode.RESOURCE_NOT_FOUND, arguments);

        // When
        arguments[0] = "changed-input";
        Object[] returnedArguments = exception.arguments();
        returnedArguments[0] = "changed-output";

        // Then
        assertThat(exception.arguments()).containsExactly("gateway-group-1");
        assertThat(exception).hasMessage("Resource gateway-group-1 does not exist");
    }

    @Test
    void shouldTreatNullArgumentsAsEmpty() {
        // Given & When
        BusinessException exception = new BusinessException(TestResponseCode.OPERATION_FAILED, (Object[]) null);

        // Then
        assertThat(exception.arguments()).isEmpty();
        assertThat(exception).hasMessage("Operation failed");
    }

    @Test
    void shouldRejectMissingResponseCode() {
        // When & Then
        assertThatThrownBy(() -> new BusinessException(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("responseCode must not be null");
    }

    private enum TestResponseCode implements ResponseCode {

        RESOURCE_NOT_FOUND("TEST-0001", "test.resource.not-found", "Resource {0} does not exist"),
        OPERATION_FAILED("TEST-0002", "test.operation.failed", "Operation failed");

        private final String code;
        private final String messageKey;
        private final String defaultMessage;

        TestResponseCode(String code, String messageKey, String defaultMessage) {
            this.code = code;
            this.messageKey = messageKey;
            this.defaultMessage = defaultMessage;
        }

        @Override
        public String code() {
            return this.code;
        }

        @Override
        public String messageKey() {
            return this.messageKey;
        }

        @Override
        public String defaultMessage() {
            return this.defaultMessage;
        }
    }
}
