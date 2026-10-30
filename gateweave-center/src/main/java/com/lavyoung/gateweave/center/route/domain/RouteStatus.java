package com.lavyoung.gateweave.center.route.domain;

/**
 * 定义控制面路由的编辑状态。
 *
 * <p>该状态决定路由是否可以进入新的发布快照，不表示某个历史快照中的运行状态。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/30
 */
public enum RouteStatus {

    /**
     * 路由可以参与后续配置发布。
     */
    ENABLED,

    /**
     * 路由不再进入新的发布快照。
     */
    DISABLED
}
