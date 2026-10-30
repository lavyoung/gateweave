package com.lavyoung.gateweave.center.route.domain;

/**
 * 定义控制面路由对调用方身份认证的要求。
 *
 * <p>该类型属于 Center 路由编辑领域。发布路由时由适配器转换为
 * contract 中的 RouteAuthenticationMode。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/30
 */
public enum RouteAuthenticationRequirement {

    /**
     * 允许未携带身份凭据的请求访问。
     */
    ANONYMOUS,

    /**
     * 请求必须携带有效身份凭据。
     */
    REQUIRED
}
