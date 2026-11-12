package com.lavyoung.gateweave.contract.http;

import java.net.URI;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HttpEndpointTests {

    @Test
    void shouldAcceptHttpAndHttpsEndpoints() {
        // When
        HttpEndpoint httpEndpoint = new HttpEndpoint(URI.create("http://127.0.0.1:8080/control-plane"));
        HttpEndpoint httpsEndpoint = new HttpEndpoint(URI.create("https://gateway.example.com"));

        // Then
        assertThat(httpEndpoint.uri()).isEqualTo(URI.create("http://127.0.0.1:8080/control-plane"));
        assertThat(httpsEndpoint.uri()).isEqualTo(URI.create("https://gateway.example.com"));
    }

    @Test
    void shouldRejectMissingEndpoint() {
        // When & Then
        assertThatThrownBy(() -> new HttpEndpoint(null)).isInstanceOf(NullPointerException.class)
                .hasMessage("uri must not be null");
    }

    @Test
    void shouldRejectRelativeUnsupportedAndHostlessEndpoints() {
        // When & Then
        assertThatThrownBy(() -> endpoint("/control-plane")).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("uri must be an absolute HTTP or HTTPS URI");
        assertThatThrownBy(() -> endpoint("ftp://gateway.example.com")).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("uri must be an absolute HTTP or HTTPS URI");
        assertThatThrownBy(() -> endpoint("http:/control-plane")).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("uri must be an absolute HTTP or HTTPS URI");
    }

    @Test
    void shouldRejectEndpointContainingUserInformation() {
        // When & Then
        assertThatThrownBy(() -> endpoint("http://user:password@gateway.example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("uri must not contain user information");
    }

    @Test
    void shouldRejectEndpointContainingQueryOrFragment() {
        // When & Then
        assertThatThrownBy(() -> endpoint("http://gateway.example.com?token=secret"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("uri must not contain query or fragment");
        assertThatThrownBy(() -> endpoint("http://gateway.example.com#internal"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("uri must not contain query or fragment");
    }

    private static HttpEndpoint endpoint(String uri) {
        return new HttpEndpoint(URI.create(uri));
    }
}
