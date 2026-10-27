package com.lavyoung.gateweave.engine.application.service;

import com.lavyoung.gateweave.contract.extension.InvocationRequest;
import com.lavyoung.gateweave.engine.application.command.GatewayRequestCommand;
import com.lavyoung.gateweave.engine.application.invocation.ProtocolInvocationDispatcher;
import com.lavyoung.gateweave.engine.application.mapper.InvocationRequestMapper;
import com.lavyoung.gateweave.engine.application.port.in.HandleGatewayRequestUseCase;
import com.lavyoung.gateweave.engine.application.result.GatewayResponse;
import com.lavyoung.gateweave.engine.application.session.GatewaySession;
import com.lavyoung.gateweave.engine.application.session.GatewaySessionFactory;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;


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

    private final GatewaySessionFactory sessionFactory;
    private final InvocationRequestMapper requestMapper;
    private final ProtocolInvocationDispatcher invocationDispatcher;

    /**
     * 创建网关请求应用服务。
     *
     * @param sessionFactory       网关会话工厂
     * @param requestMapper        协议调用请求转换器
     * @param invocationDispatcher 协议调用分派器
     * @throws NullPointerException 任一依赖为 null 时抛出
     */
    public DefaultGatewayRequestService(GatewaySessionFactory sessionFactory, InvocationRequestMapper requestMapper,
                                        ProtocolInvocationDispatcher invocationDispatcher) {
        this.sessionFactory = Objects.requireNonNull(sessionFactory, "sessionFactory must not be null");
        this.requestMapper = Objects.requireNonNull(requestMapper, "requestMapper must not be null");
        this.invocationDispatcher = Objects.requireNonNull(invocationDispatcher, "invocationDispatcher must not be null");
    }

    @Override
    public CompletionStage<GatewayResponse> handle(GatewayRequestCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        return this.sessionFactory.openSession(command)
                .map(this::invoke)
                .orElseGet(() -> CompletableFuture.completedFuture(GatewayResponse.text(404, "Route Not Found")));
    }

    /**
     * 将已建立的网关会话交给协议插件执行。
     *
     * @param session 已完成路由匹配的网关会话
     * @return 协议调用完成后的网关响应
     */
    private CompletionStage<GatewayResponse> invoke(GatewaySession session) {
        InvocationRequest request = this.requestMapper.map(session);
        CompletionStage<String> resultStage = this.invocationDispatcher.invoke(request);
        return resultStage.thenApply(result -> GatewayResponse.text(200, result));
    }
}
