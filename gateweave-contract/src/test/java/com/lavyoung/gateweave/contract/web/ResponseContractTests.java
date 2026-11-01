package com.lavyoung.gateweave.contract.web;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.lavyoung.gateweave.contract.error.CatalogResponseCode;
import com.lavyoung.gateweave.contract.error.CommonResponseCode;
import com.lavyoung.gateweave.contract.error.GatewayResponseCode;
import com.lavyoung.gateweave.contract.error.ResponseCode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResponseContractTests {

    @Test
    void shouldKeepResponseCodesAndMessageKeysUnique() {
        // Given
        List<ResponseCode> responseCodes = responseCodes();

        // When & Then
        assertThat(responseCodes).extracting(ResponseCode::code).doesNotHaveDuplicates();
        assertThat(responseCodes).extracting(ResponseCode::messageKey).doesNotHaveDuplicates();
        assertThat(responseCodes).extracting(ResponseCode::defaultMessage).allSatisfy(message ->
                assertThat(message).isNotBlank());
    }

    @Test
    void shouldCreateSuccessResultWithDefaultMessage() {
        // When
        Result<String> result = Result.success("payload");

        // Then
        assertThat(result.success()).isTrue();
        assertThat(result.code()).isEqualTo(CommonResponseCode.SUCCESS.code());
        assertThat(result.message()).isEqualTo(CommonResponseCode.SUCCESS.defaultMessage());
        assertThat(result.data()).isEqualTo("payload");
    }

    @Test
    void shouldCreateFailureResultWithLocalizedMessage() {
        // Given
        ErrorDetails details = new ErrorDetails("/gateways", "request-1");

        // When
        Result<ErrorDetails> result = Result.failure(
                GatewayResponseCode.GATEWAY_GROUP_NOT_FOUND, "Gateway group test does not exist", details);

        // Then
        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo("GATEWAY-0001");
        assertThat(result.message()).isEqualTo("Gateway group test does not exist");
        assertThat(result.data()).isSameAs(details);
    }

    @Test
    void shouldRejectSuccessCodeForFailureResult() {
        // Given
        ErrorDetails details = new ErrorDetails("/gateways", "request-1");

        // When & Then
        assertThatThrownBy(() -> Result.failure(CommonResponseCode.SUCCESS, details))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("success status and code must be consistent");
    }

    @Test
    void shouldRejectMissingResultData() {
        // When & Then
        assertThatThrownBy(() -> Result.success(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("data must not be null");
    }

    @Test
    void shouldRejectBlankResultCodeAndMessage() {
        // When & Then
        assertThatThrownBy(() -> new Result<>(false, " ", "failure", "details"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("code must not be blank");
        assertThatThrownBy(() -> new Result<>(false, "COMMON-0001", " ", "details"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("message must not be blank");
    }

    @Test
    void shouldRejectBlankErrorDetails() {
        // When & Then
        assertThatThrownBy(() -> new ErrorDetails(" ", "request-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("path must not be blank");
        assertThatThrownBy(() -> new ErrorDetails("/gateways", " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("requestId must not be blank");
    }

    private static List<ResponseCode> responseCodes() {
        return Stream.concat(
                        Stream.concat(
                                Arrays.stream(CommonResponseCode.values()).map(ResponseCode.class::cast),
                                Arrays.stream(GatewayResponseCode.values()).map(ResponseCode.class::cast)),
                        Arrays.stream(CatalogResponseCode.values()).map(ResponseCode.class::cast))
                .toList();
    }
}
