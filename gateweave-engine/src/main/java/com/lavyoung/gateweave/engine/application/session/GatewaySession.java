package com.lavyoung.gateweave.engine.application.session;

import com.lavyoung.gateweave.engine.application.command.GatewayRequestCommand;
import com.lavyoung.gateweave.engine.domain.route.GatewayRoute;

import java.util.Objects;

/**
 * 表示一次网关请求已经完成路由匹配后的会话快照。
 *
 * <p>会话将与网络框架无关的请求命令和命中的领域路由绑定在一起，
 * 供后续调用请求转换和协议调用流程使用。每次 HTTP 请求创建一个独立会话，
 * 会话不持有 Netty、Spring 或协议厂商对象。</p>
 *
 * @param command 当前请求的不可变应用命令
 * @param route   当前请求命中的领域路由
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/27
 */
public record GatewaySession(GatewayRequestCommand command, GatewayRoute route) {

    /**
     * 校验构成会话的请求命令和路由。
     *
     * @throws NullPointerException command 或 route 为 null 时抛出
     */
    public GatewaySession {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(route, "route must not be null");
    }
}
