package com.lavyoung.gateweave.center.web;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import com.lavyoung.gateweave.contract.web.ErrorDetails;
import com.lavyoung.gateweave.contract.web.Result;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTests {

    @AfterEach
    void resetLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void shouldHideUnexpectedExceptionDetails() {
        // Given
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        GlobalExceptionHandler handler = new GlobalExceptionHandler(new LocalizedResultFactory(messageSource()));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal-error");
        request.setAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE, "request-500");

        // When
        ResponseEntity<Result<ErrorDetails>> response = handler.handleUnexpectedException(
                new IllegalStateException("database password must stay private"), request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull().satisfies(result -> {
            assertThat(result.success()).isFalse();
            assertThat(result.code()).isEqualTo("COMMON-9999");
            assertThat(result.message()).isEqualTo("System error. Please try again later");
            assertThat(result.message()).doesNotContain("password");
            assertThat(result.data()).isEqualTo(new ErrorDetails("/internal-error", "request-500"));
        });
    }

    private static ResourceBundleMessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
        messageSource.setFallbackToSystemLocale(false);
        return messageSource;
    }
}
