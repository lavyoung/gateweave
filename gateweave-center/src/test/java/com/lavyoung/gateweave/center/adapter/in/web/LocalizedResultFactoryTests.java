package com.lavyoung.gateweave.center.adapter.in.web;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import com.lavyoung.gateweave.contract.error.CommonResponseCode;
import com.lavyoung.gateweave.contract.error.GatewayResponseCode;
import com.lavyoung.gateweave.contract.error.ResponseCode;
import com.lavyoung.gateweave.contract.web.ErrorDetails;
import com.lavyoung.gateweave.contract.web.Result;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalizedResultFactoryTests {

    private final ResourceBundleMessageSource messageSource = messageSource();
    private final LocalizedResultFactory resultFactory = new LocalizedResultFactory(this.messageSource);

    @AfterEach
    void resetLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void shouldResolveEnglishMessageAndArguments() {
        // Given
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        ErrorDetails details = new ErrorDetails("/gateway-groups/test/nodes", "request-1");

        // When
        Result<ErrorDetails> result = this.resultFactory.failure(
                GatewayResponseCode.GATEWAY_GROUP_NOT_FOUND, details, "test");

        // Then
        assertThat(result.code()).isEqualTo("GATEWAY-0001");
        assertThat(result.message()).isEqualTo("Gateway group test does not exist");
        assertThat(result.data()).isSameAs(details);
    }

    @Test
    void shouldResolveSimplifiedChineseMessage() {
        // Given
        LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);

        // When
        Result<String> result = this.resultFactory.success("payload");

        // Then
        assertThat(result.message()).isEqualTo("成功");
    }

    @Test
    void shouldCreateLocalizedFailureResponseFromRequestContext() {
        // Given
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/gateway-nodes");
        request.setAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE, "request-group-missing");

        // When
        ResponseEntity<Result<ErrorDetails>> response = this.resultFactory.failureResponse(
                HttpStatus.NOT_FOUND, GatewayResponseCode.GATEWAY_GROUP_NOT_FOUND, request, "missing-group");

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull().satisfies(result -> {
            assertThat(result.code()).isEqualTo("GATEWAY-0001");
            assertThat(result.message()).isEqualTo("Gateway group missing-group does not exist");
            assertThat(result.data()).isEqualTo(
                    new ErrorDetails("/api/v1/gateway-nodes", "request-group-missing"));
        });
    }

    @Test
    void shouldRejectMissingFailureResponseArguments() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/gateway-nodes");

        // When & Then
        assertThatThrownBy(() -> this.resultFactory.failureResponse(
                null, GatewayResponseCode.GATEWAY_GROUP_NOT_FOUND, request))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("status must not be null");
        assertThatThrownBy(() -> this.resultFactory.failureResponse(
                HttpStatus.NOT_FOUND, GatewayResponseCode.GATEWAY_GROUP_NOT_FOUND, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("request must not be null");
    }

    @Test
    void shouldProvideEnglishAndChineseResourcesForEveryResponseCode() {
        // Given
        List<ResponseCode> responseCodes = responseCodes();

        // When & Then
        assertThat(responseCodes).allSatisfy(responseCode -> {
            assertThat(this.resolve(responseCode, Locale.ENGLISH)).isNotBlank();
            assertThat(this.resolve(responseCode, Locale.SIMPLIFIED_CHINESE)).isNotBlank();
        });
    }

    private String resolve(ResponseCode responseCode, Locale locale) {
        return this.messageSource.getMessage(responseCode.messageKey(), new Object[]{"value"}, locale);
    }

    private static ResourceBundleMessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
        messageSource.setFallbackToSystemLocale(false);
        return messageSource;
    }

    private static List<ResponseCode> responseCodes() {
        return Stream.concat(
                        Arrays.stream(CommonResponseCode.values()), Arrays.stream(GatewayResponseCode.values()))
                .map(ResponseCode.class::cast)
                .toList();
    }
}
