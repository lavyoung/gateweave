package com.lavyoung.gateweave.engine.application.service;

import com.lavyoung.gateweave.engine.application.command.GatewayRequestCommand;
import com.lavyoung.gateweave.engine.application.port.in.HandleGatewayRequestUseCase;
import com.lavyoung.gateweave.engine.application.result.GatewayResponse;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * 提供当前阶段默认的网关请求处理流程。
 *
 * <p>第 1 章尚未实现路由匹配和下游服务调用，因此该服务仅返回请求方法与
 * URI，用于验证从网络适配器到应用层再到 HTTP 响应的完整链路。</p>
 *
 * <p>该类保持无状态且不依赖 Spring、Netty，后续可以在装配层中将其注册为
 * 单例 Bean，而不污染应用层。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/25
 */
public final class DefaultGatewayRequestService implements HandleGatewayRequestUseCase {

    @Override
    public CompletionStage<GatewayResponse> handle(GatewayRequestCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        String content = "GateWeave received request: %s %s".formatted(command.method(), command.uri());
        return CompletableFuture.completedFuture(GatewayResponse.text(200, content));
    }
}
