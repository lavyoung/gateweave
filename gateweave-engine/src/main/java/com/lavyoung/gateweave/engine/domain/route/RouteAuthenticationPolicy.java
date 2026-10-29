package com.lavyoung.gateweave.engine.domain.route;

/**
 * 表示数据面执行路由时采用的身份认证策略。
 *
 * <p>该策略属于网关数据面的领域模型，不依赖控制面配置契约，
 * 也不感知 JWT、Shiro 或 HTTP Authorization 请求头。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/29
 */
public enum RouteAuthenticationPolicy {

    /**
     * 请求无需认证即可进入协议调用流程。
     */
    ANONYMOUS,

    /**
     * 请求必须具有已经验证的调用主体。
     */
    REQUIRED
}
