package com.lavyoung.gateweave.engine.application.mapper;

import java.net.URI;
import java.net.http.HttpHeaders;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import com.lavyoung.gateweave.engine.application.command.GatewayRequestCommand;
import com.lavyoung.gateweave.engine.application.command.HttpParameterValues;
import com.lavyoung.gateweave.engine.domain.invocation.InvocationParameter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GatewayInvocationArgumentsResolverTests {

    private final GatewayInvocationArgumentsResolver resolver =
            new GatewayInvocationArgumentsResolver(new ObjectMapper());

    @Test
    void shouldResolveScalarParametersInDeclaredOrder() {
        // Given
        var command = command("GET", Map.of("age", List.of("18"), "name", List.of("lavyoung")));
        var parameters = List.of(
                new InvocationParameter("name", String.class.getName()),
                new InvocationParameter("age", Integer.class.getName()));

        // When
        var invocationArguments = this.resolver.resolve(command, parameters);

        // Then
        assertThat(invocationArguments.parameterTypes())
                .containsExactly(String.class.getName(), Integer.class.getName());
        assertThat(invocationArguments.arguments()).containsExactly("lavyoung", "18");
    }

    @Test
    void shouldKeepAllValuesForListParameter() {
        // Given
        var command = command("GET", Map.of("tag", List.of("java", "netty")));
        var parameters = List.of(new InvocationParameter("tag", List.class.getName()));

        // When
        var invocationArguments = this.resolver.resolve(command, parameters);

        // Then
        assertThat(invocationArguments.parameterTypes()).containsExactly(List.class.getName());
        assertThat(invocationArguments.arguments()).containsExactly(List.of("java", "netty"));
    }

    @Test
    void shouldKeepAllValuesForSetParameter() {
        // Given
        var command = command("GET", Map.of("role", List.of("admin", "developer")));
        var parameters = List.of(new InvocationParameter("role", Set.class.getName()));

        // When
        var invocationArguments = this.resolver.resolve(command, parameters);

        // Then
        assertThat(invocationArguments.parameterTypes()).containsExactly(Set.class.getName());
        assertThat(invocationArguments.arguments()).containsExactly(List.of("admin", "developer"));
    }

    @Test
    void shouldKeepAllValuesForArrayParameter() {
        // Given
        var command = command("GET", Map.of("id", List.of("1001", "1002")));
        var parameters = List.of(new InvocationParameter("id", String[].class.getName()));

        // When
        var invocationArguments = this.resolver.resolve(command, parameters);

        // Then
        assertThat(invocationArguments.parameterTypes()).containsExactly(String[].class.getName());
        assertThat(invocationArguments.arguments()).containsExactly(List.of("1001", "1002"));
    }

    @Test
    void shouldTreatUnavailableProviderTypeAsScalar() {
        // Given
        var command = command("GET", Map.of("user", List.of("1001")));
        var parameters = List.of(new InvocationParameter("user", "com.example.UserId"));

        // When
        var invocationArguments = this.resolver.resolve(command, parameters);

        // Then
        assertThat(invocationArguments.parameterTypes()).containsExactly("com.example.UserId");
        assertThat(invocationArguments.arguments()).containsExactly("1001");
    }

    @Test
    void shouldRejectMissingQueryParameter() {
        // Given
        var command = command("GET", Map.of());
        var parameters = List.of(new InvocationParameter("name", String.class.getName()));

        // When & Then
        assertThatThrownBy(() -> this.resolver.resolve(command, parameters))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("missing query parameter: name");
    }

    @Test
    void shouldRejectMultipleValuesForScalarParameter() {
        // Given
        var command = command("GET", Map.of("name", List.of("alice", "bob")));
        var parameters = List.of(new InvocationParameter("name", String.class.getName()));

        // When & Then
        assertThatThrownBy(() -> this.resolver.resolve(command, parameters))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("query parameter must have exactly one value: name");
    }

    @Test
    void shouldResolveSinglePostObjectAsOneArgument() {
        // Given
        var command = command("POST", "{\"name\":\"lavyoung\",\"age\":19}");
        var parameters = List.of(new InvocationParameter("request", "com.example.UserRequest"));

        // When
        var invocationArguments = this.resolver.resolve(command, parameters);

        // Then
        assertThat(invocationArguments.parameterTypes()).containsExactly("com.example.UserRequest");
        assertThat(invocationArguments.arguments()).containsExactly(Map.of("name", "lavyoung", "age", 19));
    }

    @Test
    void shouldResolveMultiplePostParametersInDeclaredOrder() {
        // Given
        var command = command("POST", "{\"age\":19,\"name\":\"lavyoung\"}");
        var parameters = List.of(
                new InvocationParameter("name", String.class.getName()),
                new InvocationParameter("age", Integer.class.getName()));

        // When
        var invocationArguments = this.resolver.resolve(command, parameters);

        // Then
        assertThat(invocationArguments.parameterTypes())
                .containsExactly(String.class.getName(), Integer.class.getName());
        assertThat(invocationArguments.arguments()).containsExactly("lavyoung", 19);
    }

    @Test
    void shouldResolvePostArrayAsSingleArgument() {
        // Given
        var command = command("POST", "[\"java\",\"netty\"]");
        var parameters = List.of(new InvocationParameter("tags", List.class.getName()));

        // When
        var invocationArguments = this.resolver.resolve(command, parameters);

        // Then
        assertThat(invocationArguments.parameterTypes()).containsExactly(List.class.getName());
        assertThat(invocationArguments.arguments()).containsExactly(List.of("java", "netty"));
    }

    @Test
    void shouldResolveUrlEncodedFormParametersInDeclaredOrder() {
        // Given
        var command = formCommand("application/x-www-form-urlencoded; charset=UTF-8",
                Map.of("age", List.of("19"), "name", List.of("lavyoung")));
        var parameters = List.of(
                new InvocationParameter("name", String.class.getName()),
                new InvocationParameter("age", Integer.class.getName()));

        // When
        var invocationArguments = this.resolver.resolve(command, parameters);

        // Then
        assertThat(invocationArguments.parameterTypes())
                .containsExactly(String.class.getName(), Integer.class.getName());
        assertThat(invocationArguments.arguments()).containsExactly("lavyoung", "19");
    }

    @Test
    void shouldKeepAllMultipartValuesForCollectionParameter() {
        // Given
        var command = formCommand("multipart/form-data; boundary=GateWeaveBoundary",
                Map.of("tag", List.of("java", "netty")));
        var parameters = List.of(new InvocationParameter("tag", Set.class.getName()));

        // When
        var invocationArguments = this.resolver.resolve(command, parameters);

        // Then
        assertThat(invocationArguments.parameterTypes()).containsExactly(Set.class.getName());
        assertThat(invocationArguments.arguments()).containsExactly(List.of("java", "netty"));
    }

    @Test
    void shouldRejectMissingFormParameter() {
        // Given
        var command = formCommand("application/x-www-form-urlencoded", Map.of());
        var parameters = List.of(new InvocationParameter("name", String.class.getName()));

        // When & Then
        assertThatThrownBy(() -> this.resolver.resolve(command, parameters))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("missing form parameter: name");
    }

    @Test
    void shouldResolvePostWithoutParametersWithoutReadingBody() {
        // Given
        var command = command("POST", "");

        // When
        var invocationArguments = this.resolver.resolve(command, List.of());

        // Then
        assertThat(invocationArguments.parameterTypes()).isEmpty();
        assertThat(invocationArguments.arguments()).isEmpty();
    }

    @Test
    void shouldRejectEmptyPostBodyWhenParameterExists() {
        // Given
        var command = command("POST", "");
        var parameters = List.of(new InvocationParameter("request", "com.example.UserRequest"));

        // When & Then
        assertThatThrownBy(() -> this.resolver.resolve(command, parameters))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("POST request body must not be empty");
    }

    @Test
    void shouldRejectInvalidPostJson() {
        // Given
        var command = command("POST", "{invalid-json}");
        var parameters = List.of(new InvocationParameter("request", "com.example.UserRequest"));

        // When & Then
        assertThatThrownBy(() -> this.resolver.resolve(command, parameters))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("POST request body must be valid JSON")
                .hasCauseInstanceOf(Exception.class);
    }

    @Test
    void shouldRejectPostWithoutContentType() {
        // Given
        var command = command("POST", "{\"name\":\"lavyoung\"}", null);
        var parameters = List.of(new InvocationParameter("request", "com.example.UserRequest"));

        // When & Then
        assertThatThrownBy(() -> this.resolver.resolve(command, parameters))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("POST request Content-Type is required");
    }

    @Test
    void shouldRejectUnsupportedPostContentType() {
        // Given
        var command = command("POST", "GateWeave", "text/plain; charset=UTF-8");
        var parameters = List.of(new InvocationParameter("request", String.class.getName()));

        // When & Then
        assertThatThrownBy(() -> this.resolver.resolve(command, parameters))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("unsupported POST request Content-Type: text/plain; charset=UTF-8");
    }

    @Test
    void shouldRejectInvalidPostContentType() {
        // Given
        var command = command("POST", "{}", "invalid content type");
        var parameters = List.of(new InvocationParameter("request", "com.example.UserRequest"));

        // When & Then
        assertThatThrownBy(() -> this.resolver.resolve(command, parameters))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("invalid POST request Content-Type: invalid content type")
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectNonObjectPostBodyForMultipleParameters() {
        // Given
        var command = command("POST", "[\"lavyoung\",19]");
        var parameters = List.of(
                new InvocationParameter("name", String.class.getName()),
                new InvocationParameter("age", Integer.class.getName()));

        // When & Then
        assertThatThrownBy(() -> this.resolver.resolve(command, parameters))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("POST request body must be a JSON object for multiple parameters");
    }

    @Test
    void shouldRejectMissingPostParameter() {
        // Given
        var command = command("POST", "{\"name\":\"lavyoung\"}");
        var parameters = List.of(
                new InvocationParameter("name", String.class.getName()),
                new InvocationParameter("age", Integer.class.getName()));

        // When & Then
        assertThatThrownBy(() -> this.resolver.resolve(command, parameters))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("missing body parameter: age");
    }

    @Test
    void shouldRejectUnsupportedHttpMethod() {
        // Given
        var command = command("PUT", Map.of());

        // When & Then
        assertThatThrownBy(() -> this.resolver.resolve(command, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("unsupported HTTP method for parameter binding: PUT");
    }

    @Test
    void shouldRejectNullInputs() {
        // Given
        var command = command("GET", Map.of());

        // When & Then
        assertThatThrownBy(() -> this.resolver.resolve(null, List.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("command must not be null");
        assertThatThrownBy(() -> this.resolver.resolve(command, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("parameters must not be null");
        assertThatThrownBy(() -> new GatewayInvocationArgumentsResolver(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("objectMapper must not be null");
    }

    private static GatewayRequestCommand command(String method, Map<String, List<String>> queryParameters) {
        var headers = HttpHeaders.of(Map.of(), (name, value) -> true);
        return new GatewayRequestCommand("request-1", method, URI.create("/search"), headers,
                new HttpParameterValues(queryParameters), new byte[0]);
    }

    private static GatewayRequestCommand command(String method, String body) {
        return command(method, body, "application/json; charset=UTF-8");
    }

    private static GatewayRequestCommand command(String method, String body, String contentType) {
        Map<String, List<String>> headerValues = contentType == null
                ? Map.of() : Map.of("Content-Type", List.of(contentType));
        var headers = HttpHeaders.of(headerValues, (name, value) -> true);
        return new GatewayRequestCommand("request-1", method, URI.create("/invoke"), headers,
                new HttpParameterValues(Map.of()), body.getBytes(StandardCharsets.UTF_8));
    }

    private static GatewayRequestCommand formCommand(String contentType, Map<String, List<String>> formParameters) {
        var headers = HttpHeaders.of(Map.of("Content-Type", List.of(contentType)), (name, value) -> true);
        return new GatewayRequestCommand("request-1", "POST", URI.create("/invoke"), headers,
                new HttpParameterValues(Map.of()), new HttpParameterValues(formParameters), new byte[0]);
    }
}
