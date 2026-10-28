package com.lavyoung.gateweave.engine.application.mapper;

import com.lavyoung.gateweave.contract.extension.GenericInvocationArguments;
import com.lavyoung.gateweave.contract.extension.InvocationRequest;
import com.lavyoung.gateweave.engine.application.command.GatewayRequestCommand;
import com.lavyoung.gateweave.engine.application.session.GatewaySession;
import com.lavyoung.gateweave.engine.domain.invocation.InvocationTarget;
import com.lavyoung.gateweave.engine.domain.route.GatewayRoute;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 将已经完成路由匹配的网关会话转换为协议调用契约。
 *
 * <p>该转换器负责调用参数绑定，并跨越应用层与协议插件之间的模型边界。
 * 它不执行路由匹配，也不选择或调用具体协议插件。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/27
 */
public final class InvocationRequestMapper {

    private final GatewayInvocationArgumentsResolver argumentsResolver;

    /**
     * 创建协议调用请求转换器。
     *
     * @param argumentsResolver HTTP 请求到调用参数的解析器
     * @throws NullPointerException argumentsResolver 为 null 时抛出
     */
    public InvocationRequestMapper(GatewayInvocationArgumentsResolver argumentsResolver) {
        this.argumentsResolver = Objects.requireNonNull(argumentsResolver, "argumentsResolver must not be null");
    }

    /**
     * 根据网关会话构造不可变调用请求。
     *
     * @param session 已经完成路由匹配的网关会话
     * @return 可交给协议插件执行的调用请求
     * @throws NullPointerException     session 为 null 时抛出
     * @throws IllegalArgumentException 请求参数无法与目标方法参数绑定时抛出
     */
    public InvocationRequest map(GatewaySession session) {
        Objects.requireNonNull(session, "session must not be null");
        GatewayRequestCommand command = session.command();
        GatewayRoute route = session.route();
        InvocationTarget target = route.target();
        GenericInvocationArguments invocationArguments = this.argumentsResolver.resolve(command, target.parameters());
        Map<String, String> headers = command.headers().map().entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(
                        Map.Entry::getKey, entry -> String.join(", ", entry.getValue())));

        return new InvocationRequest(command.requestId(), route.routeId(), target.protocol(), target.service(),
                target.operation(), invocationArguments, target.timeout(), headers, target.attributes(),
                command.body());
    }
}
