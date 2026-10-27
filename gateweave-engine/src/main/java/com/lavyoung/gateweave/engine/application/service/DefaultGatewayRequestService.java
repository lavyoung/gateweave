package com.lavyoung.gateweave.engine.application.service;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import com.lavyoung.gateweave.contract.extension.IGenericReference;
import com.lavyoung.gateweave.engine.application.command.GatewayRequestCommand;
import com.lavyoung.gateweave.engine.application.port.in.HandleGatewayRequestUseCase;
import com.lavyoung.gateweave.engine.application.result.GatewayResponse;
import com.lavyoung.gateweave.engine.domain.invocation.InvocationTarget;
import com.lavyoung.gateweave.engine.domain.route.GatewayRoute;
import com.lavyoung.gateweave.engine.domain.route.GatewayRouteTable;

/**
 * 完成网关 HTTP 请求到 RPC 泛化调用的应用层编排。
 *
 * <p>该服务负责路由匹配、泛化代理解析和异步结果转换，不依赖 Netty、
 * Spring 或 Dubbo SDK。当前章节将 HTTP 请求体作为 RPC 的字符串参数。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @since 1.0
 */
public final class DefaultGatewayRequestService implements HandleGatewayRequestUseCase {

    private final GatewayRouteTable routeTable;
    private final Function<InvocationTarget, IGenericReference> genericReferenceResolver;

    /**
     * 创建网关请求应用服务。
     *
     * @param routeTable               当前生效的路由表
     * @param genericReferenceResolver 根据完整调用目标解析泛化调用代理的函数
     * @throws NullPointerException 任一依赖为 null 时抛出
     */
    public DefaultGatewayRequestService(
            GatewayRouteTable routeTable, Function<InvocationTarget, IGenericReference> genericReferenceResolver) {
        this.routeTable = Objects.requireNonNull(routeTable, "routeTable must not be null");
        this.genericReferenceResolver =
                Objects.requireNonNull(genericReferenceResolver, "genericReferenceResolver must not be null");
    }

    @Override
    public CompletionStage<GatewayResponse> handle(GatewayRequestCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        return this.routeTable.match(command.method(), command.uri())
                .map(route -> invoke(command, route))
                .orElseGet(() -> CompletableFuture.completedFuture(GatewayResponse.text(404, "Route Not Found")));
    }

    /**
     * 把请求体转换为字符串参数并执行路由绑定的 RPC 方法。
     *
     * @param command 当前网关请求
     * @param route   已匹配的网关路由
     * @return RPC 调用完成后的网关响应
     * @throws NullPointerException 泛化代理或异步阶段为 null 时抛出
     */
    private CompletionStage<GatewayResponse> invoke(GatewayRequestCommand command, GatewayRoute route) {
        IGenericReference reference = this.genericReferenceResolver.apply(route.target());
        Objects.requireNonNull(reference, "genericReferenceResolver must not return null");
        String argument = new String(command.body(), StandardCharsets.UTF_8);
        CompletionStage<String> resultStage = reference.$invoke(argument);
        Objects.requireNonNull(resultStage, "generic reference must not return null");
        return resultStage.thenApply(result -> GatewayResponse.text(200, result));
    }
}
