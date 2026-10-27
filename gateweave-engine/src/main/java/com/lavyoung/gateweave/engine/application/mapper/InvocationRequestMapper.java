package com.lavyoung.gateweave.engine.application.mapper;

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
 * <p>该转换器负责跨越应用层与协议插件之间的模型边界。
 * 它只读取会话中的请求和路由快照，不执行路由匹配，
 * 也不选择或调用具体协议插件。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/27
 */
public final class InvocationRequestMapper {

    /**
     * 根据网关会话构造不可变调用请求。
     *
     * @param session 已经完成路由匹配的网关会话
     * @return 可交给协议插件执行的调用请求
     * @throws NullPointerException session 为 null 时抛出
     */
    public InvocationRequest map(GatewaySession session) {
        Objects.requireNonNull(session, "session must not be null");
        GatewayRequestCommand command = session.command();
        GatewayRoute route = session.route();
        InvocationTarget target = route.target();
        Map<String, String> headers = command.headers().map().entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey,
                        entry -> String.join(", ", entry.getValue())));
        return new InvocationRequest(
                command.requestId(),
                route.routeId(),
                target.protocol(),
                target.service(),
                target.operation(),
                target.parameterTypes(),
                target.timeout(),
                headers,
                target.attributes(),
                command.body()
        );
    }
}
