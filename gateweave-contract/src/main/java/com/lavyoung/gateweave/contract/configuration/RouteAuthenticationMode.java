package com.lavyoung.gateweave.contract.configuration;

/**
 * 定义控制面发布给网关数据面的路由身份认证模式。
 *
 * <p>该模式只表达路由访问要求，不暴露 Shiro、JWT 或 HTTP 请求头等实现细节。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/29
 */
public enum RouteAuthenticationMode {

    /**
     * 允许未携带身份凭据的请求访问。
     */
    ANONYMOUS,

    /**
     * 请求必须提供有效身份凭据。
     */
    REQUIRED
}
