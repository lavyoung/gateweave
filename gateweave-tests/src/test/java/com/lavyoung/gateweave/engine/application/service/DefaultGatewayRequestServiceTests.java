package com.lavyoung.gateweave.engine.application.service;

import java.net.URI;
import java.net.http.HttpHeaders;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.lavyoung.gateweave.contract.extension.InvocationRequest;
import com.lavyoung.gateweave.contract.extension.ProtocolInvoker;
import com.lavyoung.gateweave.engine.application.command.GatewayRequestCommand;
import com.lavyoung.gateweave.engine.application.command.HttpParameterValues;
import com.lavyoung.gateweave.engine.application.invocation.ProtocolInvocationDispatcher;
import com.lavyoung.gateweave.engine.application.mapper.GatewayInvocationArgumentsResolver;
import com.lavyoung.gateweave.engine.application.mapper.InvocationRequestMapper;
import com.lavyoung.gateweave.engine.application.security.GatewaySessionAuthenticationService;
import com.lavyoung.gateweave.engine.application.session.DefaultGatewaySessionFactory;
import com.lavyoung.gateweave.engine.application.session.GatewaySession;
import com.lavyoung.gateweave.engine.application.session.GatewaySessionFactory;
import com.lavyoung.gateweave.engine.domain.invocation.InvocationParameter;
import com.lavyoung.gateweave.engine.domain.invocation.InvocationTarget;
import com.lavyoung.gateweave.engine.domain.route.GatewayRoute;
import com.lavyoung.gateweave.engine.domain.route.GatewayRouteTable;
import com.lavyoung.gateweave.engine.domain.route.RouteAuthenticationPolicy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DefaultGatewayRequestServiceTests {

    private static final HttpHeaders EMPTY_HEADERS = HttpHeaders.of(Map.of(), (name, value) -> true);
    private static final InvocationTarget TARGET = new InvocationTarget(
            "dubbo", "cn.bugstack.gateway.rpc.IActivityBooth", "sayHi",
            List.of(new InvocationParameter("name", "java.lang.String")), Duration.ofSeconds(3),
            Map.of("version", "1.0.0"));
    private static final GatewayRoute ROUTE = new GatewayRoute(
            "route-say-hi", "/sayHi", "GET", "activity-service", RouteAuthenticationPolicy.ANONYMOUS,
            TARGET, Map.of());
    private static final GatewayRouteTable ROUTE_TABLE = new GatewayRouteTable(List.of(ROUTE));

    @Test
    void shouldCompleteSessionMappingAndProtocolInvocationWhenRouteMatches() {
        // Given
        ProtocolInvoker invoker = invoker();
        when(invoker.invoke(any(InvocationRequest.class)))
                .thenReturn(CompletableFuture.completedFuture("hi GateWeave"));
        DefaultGatewayRequestService service = service(invoker);
        GatewayRequestCommand command = request("GET", "/sayHi?name=GateWeave", "");

        // When
        var response = service.handle(command).toCompletableFuture().join();

        // Then
        ArgumentCaptor<InvocationRequest> requestCaptor = ArgumentCaptor.forClass(InvocationRequest.class);
        verify(invoker).invoke(requestCaptor.capture());
        InvocationRequest invocationRequest = requestCaptor.getValue();
        assertThat(invocationRequest.requestId()).isEqualTo("request-1");
        assertThat(invocationRequest.routeId()).isEqualTo("route-say-hi");
        assertThat(invocationRequest.service()).isEqualTo("cn.bugstack.gateway.rpc.IActivityBooth");
        assertThat(invocationRequest.invocationArguments().parameterTypes()).containsExactly("java.lang.String");
        assertThat(invocationRequest.invocationArguments().arguments()).containsExactly("GateWeave");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("content-type")).contains("text/plain; charset=UTF-8");
        assertThat(new String(response.body(), StandardCharsets.UTF_8)).isEqualTo("hi GateWeave");
    }

    @Test
    void shouldReturnNotFoundWithoutInvokingProtocolWhenRouteDoesNotMatch() {
        // Given
        ProtocolInvoker invoker = invoker();
        DefaultGatewayRequestService service = service(invoker);

        // When
        var response = service.handle(request("GET", "/missing", "GateWeave")).toCompletableFuture().join();

        // Then
        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(new String(response.body(), StandardCharsets.UTF_8)).isEqualTo("Route Not Found");
        verify(invoker, never()).invoke(any(InvocationRequest.class));
    }

    @Test
    void shouldReturnUnauthorizedWithoutMappingOrInvokingProtocolWhenAuthenticationFails() {
        // Given
        GatewaySessionFactory sessionFactory = new DefaultGatewaySessionFactory(ROUTE_TABLE);
        GatewaySessionAuthenticationService authenticationService = mock(GatewaySessionAuthenticationService.class);
        InvocationRequestMapper requestMapper = mock(InvocationRequestMapper.class);
        ProtocolInvocationDispatcher dispatcher = mock(ProtocolInvocationDispatcher.class);
        DefaultGatewayRequestService service = new DefaultGatewayRequestService(
                sessionFactory, requestMapper, dispatcher, authenticationService);
        when(authenticationService.authenticate(any(GatewaySession.class))).thenReturn(false);

        // When
        var response = service.handle(request("GET", "/sayHi?name=GateWeave", "")).toCompletableFuture().join();

        // Then
        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.headers().firstValue("WWW-Authenticate")).contains("Bearer");
        assertThat(response.body()).isEmpty();
        verify(authenticationService).authenticate(any(GatewaySession.class));
        verifyNoInteractions(requestMapper, dispatcher);
    }

    @Test
    void shouldPropagateProtocolInvocationFailure() {
        // Given
        IllegalStateException failure = new IllegalStateException("RPC unavailable");
        ProtocolInvoker invoker = invoker();
        when(invoker.invoke(any(InvocationRequest.class))).thenReturn(CompletableFuture.failedFuture(failure));
        DefaultGatewayRequestService service = service(invoker);

        // When & Then
        assertThatThrownBy(() -> service.handle(request("GET", "/sayHi?name=GateWeave", ""))
                .toCompletableFuture().join())
                .isInstanceOf(CompletionException.class)
                .hasCause(failure);
    }

    @Test
    void shouldRejectMissingCommandAndDependencies() {
        // Given
        ProtocolInvoker invoker = invoker();
        GatewaySessionFactory sessionFactory = new DefaultGatewaySessionFactory(ROUTE_TABLE);
        InvocationRequestMapper requestMapper = requestMapper();
        ProtocolInvocationDispatcher dispatcher = new ProtocolInvocationDispatcher(List.of(invoker));
        GatewaySessionAuthenticationService authenticationService = mock(GatewaySessionAuthenticationService.class);
        DefaultGatewayRequestService service = new DefaultGatewayRequestService(
                sessionFactory, requestMapper, dispatcher, authenticationService);

        // When & Then
        assertThatThrownBy(() -> service.handle(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("command must not be null");
        assertThatThrownBy(() -> new DefaultGatewayRequestService(
                null, requestMapper, dispatcher, authenticationService))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("sessionFactory must not be null");
        assertThatThrownBy(() -> new DefaultGatewayRequestService(
                sessionFactory, null, dispatcher, authenticationService))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("requestMapper must not be null");
        assertThatThrownBy(() -> new DefaultGatewayRequestService(
                sessionFactory, requestMapper, null, authenticationService))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("invocationDispatcher must not be null");
        assertThatThrownBy(() -> new DefaultGatewayRequestService(sessionFactory, requestMapper, dispatcher, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("authenticationService must not be null");
    }

    private static DefaultGatewayRequestService service(ProtocolInvoker invoker) {
        GatewaySessionFactory sessionFactory = new DefaultGatewaySessionFactory(ROUTE_TABLE);
        InvocationRequestMapper requestMapper = requestMapper();
        ProtocolInvocationDispatcher dispatcher = new ProtocolInvocationDispatcher(List.of(invoker));
        GatewaySessionAuthenticationService authenticationService = mock(GatewaySessionAuthenticationService.class);
        when(authenticationService.authenticate(any(GatewaySession.class))).thenReturn(true);
        return new DefaultGatewayRequestService(sessionFactory, requestMapper, dispatcher, authenticationService);
    }

    private static ProtocolInvoker invoker() {
        ProtocolInvoker invoker = mock(ProtocolInvoker.class);
        when(invoker.protocol()).thenReturn("dubbo");
        return invoker;
    }

    private static GatewayRequestCommand request(String method, String uri, String body) {
        URI requestUri = URI.create(uri);
        Map<String, List<String>> queryParameters = requestUri.getQuery() == null
                ? Map.of() : Map.of("name", List.of("GateWeave"));
        return new GatewayRequestCommand("request-1", method, requestUri, EMPTY_HEADERS,
                new HttpParameterValues(queryParameters), body.getBytes(StandardCharsets.UTF_8));
    }

    private static InvocationRequestMapper requestMapper() {
        return new InvocationRequestMapper(new GatewayInvocationArgumentsResolver(new ObjectMapper()));
    }
}
