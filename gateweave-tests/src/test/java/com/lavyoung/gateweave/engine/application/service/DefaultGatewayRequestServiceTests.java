package com.lavyoung.gateweave.engine.application.service;

import java.net.URI;
import java.net.http.HttpHeaders;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.lavyoung.gateweave.contract.extension.IGenericReference;
import com.lavyoung.gateweave.engine.application.command.GatewayRequestCommand;
import com.lavyoung.gateweave.engine.domain.invocation.InvocationTarget;
import com.lavyoung.gateweave.engine.domain.route.GatewayRoute;
import com.lavyoung.gateweave.engine.domain.route.GatewayRouteTable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultGatewayRequestServiceTests {

    private static final HttpHeaders EMPTY_HEADERS = HttpHeaders.of(Map.of(), (name, value) -> true);
    private static final InvocationTarget TARGET = new InvocationTarget(
            "dubbo", "cn.bugstack.gateway.rpc.IActivityBooth", "sayHi", List.of("java.lang.String"),
            Duration.ofSeconds(3), Map.of("version", "1.0.0"));
    private static final GatewayRoute ROUTE = new GatewayRoute(
            "route-say-hi", "/sayHi", "GET", "activity-service", TARGET, Map.of());
    private static final GatewayRouteTable ROUTE_TABLE = new GatewayRouteTable(List.of(ROUTE));

    @Test
    void shouldInvokeGenericReferenceWhenRouteMatches() {
        // Given
        var resolvedTarget = new AtomicReference<InvocationTarget>();
        IGenericReference reference = argument -> CompletableFuture.completedFuture("hi " + argument);
        var service = new DefaultGatewayRequestService(ROUTE_TABLE, target -> {
            resolvedTarget.set(target);
            return reference;
        });
        var command = request("GET", "/sayHi", "GateWeave");

        // When
        var response = service.handle(command).toCompletableFuture().join();

        // Then
        assertThat(resolvedTarget).hasValue(TARGET);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("content-type")).contains("text/plain; charset=UTF-8");
        assertThat(new String(response.body(), StandardCharsets.UTF_8)).isEqualTo("hi GateWeave");
    }

    @Test
    void shouldReturnNotFoundWithoutResolvingReferenceWhenRouteDoesNotMatch() {
        // Given
        var service = new DefaultGatewayRequestService(ROUTE_TABLE, operation -> {
            throw new AssertionError("generic reference must not be resolved");
        });
        var command = request("GET", "/missing", "GateWeave");

        // When
        var response = service.handle(command).toCompletableFuture().join();

        // Then
        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(new String(response.body(), StandardCharsets.UTF_8)).isEqualTo("Route Not Found");
    }

    @Test
    void shouldPropagateGenericInvocationFailure() {
        // Given
        var failure = new IllegalStateException("RPC unavailable");
        IGenericReference reference = argument -> CompletableFuture.failedFuture(failure);
        var service = new DefaultGatewayRequestService(ROUTE_TABLE, operation -> reference);

        // When & Then
        assertThatThrownBy(() -> service.handle(request("GET", "/sayHi", "GateWeave"))
                .toCompletableFuture().join())
                .isInstanceOf(CompletionException.class)
                .hasCause(failure);
    }

    @Test
    void shouldRejectMissingCommandAndDependencies() {
        // Given
        var service = new DefaultGatewayRequestService(ROUTE_TABLE, operation -> argument ->
                CompletableFuture.completedFuture(argument));

        // When & Then
        assertThatThrownBy(() -> service.handle(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("command must not be null");
        assertThatThrownBy(() -> new DefaultGatewayRequestService(null, operation -> null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("routeTable must not be null");
        assertThatThrownBy(() -> new DefaultGatewayRequestService(ROUTE_TABLE, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("genericReferenceResolver must not be null");
    }

    private static GatewayRequestCommand request(String method, String uri, String body) {
        return new GatewayRequestCommand(
                "request-1", method, URI.create(uri), EMPTY_HEADERS, body.getBytes(StandardCharsets.UTF_8));
    }
}
