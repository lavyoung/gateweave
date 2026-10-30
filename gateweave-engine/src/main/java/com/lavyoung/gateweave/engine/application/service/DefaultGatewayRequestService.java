package com.lavyoung.gateweave.engine.application.service;

import java.net.http.HttpHeaders;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import com.lavyoung.gateweave.contract.extension.InvocationRequest;
import com.lavyoung.gateweave.engine.application.command.GatewayRequestCommand;
import com.lavyoung.gateweave.engine.application.invocation.ProtocolInvocationDispatcher;
import com.lavyoung.gateweave.engine.application.mapper.InvocationRequestMapper;
import com.lavyoung.gateweave.engine.application.port.in.HandleGatewayRequestUseCase;
import com.lavyoung.gateweave.engine.application.result.GatewayResponse;
import com.lavyoung.gateweave.engine.application.security.GatewaySessionAuthenticationService;
import com.lavyoung.gateweave.engine.application.session.GatewaySession;
import com.lavyoung.gateweave.engine.application.session.GatewaySessionFactory;


/**
 * 编排一次完整的网关请求会话。
 *
 * <p>该服务依次完成会话创建、调用请求映射、协议调用分派和网关响应转换。
 * 路由匹配、协议选择和具体 RPC 调用分别委托给对应组件处理。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @since 1.0
 */
public final class DefaultGatewayRequestService implements HandleGatewayRequestUseCase {

    private static final GatewayResponse UNAUTHORIZED_RESPONSE = new GatewayResponse(
            401, HttpHeaders.of(Map.of("WWW-Authenticate", List.of("Bearer")), (name, value) -> true), new byte[0]);

    private final GatewaySessionFactory sessionFactory;
    private final InvocationRequestMapper requestMapper;
    private final ProtocolInvocationDispatcher invocationDispatcher;
    private final GatewaySessionAuthenticationService authenticationService;

    /**
     * 创建网关请求应用服务。
     *
     * @param sessionFactory        网关会话工厂
     * @param requestMapper         协议调用请求转换器
     * @param invocationDispatcher  协议调用分派器
     * @param authenticationService 会话鉴权应用服务
     * @throws NullPointerException 任一依赖为 null 时抛出
     */
    public DefaultGatewayRequestService(
            GatewaySessionFactory sessionFactory, InvocationRequestMapper requestMapper,
            ProtocolInvocationDispatcher invocationDispatcher,
            GatewaySessionAuthenticationService authenticationService) {
        this.sessionFactory = Objects.requireNonNull(sessionFactory, "sessionFactory must not be null");
        this.requestMapper = Objects.requireNonNull(requestMapper, "requestMapper must not be null");
        this.invocationDispatcher = Objects.requireNonNull(
                invocationDispatcher, "invocationDispatcher must not be null");
        this.authenticationService = Objects.requireNonNull(
                authenticationService, "authenticationService must not be null");
    }

    @Override
    public CompletionStage<GatewayResponse> handle(GatewayRequestCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        return this.sessionFactory.openSession(command)
                .map(this::authenticateAndInvoke)
                .orElseGet(() -> CompletableFuture.completedFuture(GatewayResponse.text(404, "Route Not Found")));
    }

    /**
     * 对已经完成路由匹配的会话执行鉴权然后交给协议插件执行。
     *
     * @param session 已完成路由匹配的网关会话
     * @return 协议调用完成后的网关响应
     */
    private CompletionStage<GatewayResponse> authenticateAndInvoke(GatewaySession session) {
        if (!this.authenticationService.authenticate(session)) {
            return CompletableFuture.completedFuture(UNAUTHORIZED_RESPONSE);
        }
        InvocationRequest request = this.requestMapper.map(session);
        CompletionStage<String> resultStage = this.invocationDispatcher.invoke(request);
        return resultStage.thenApply(result -> GatewayResponse.text(200, result));
    }
}
