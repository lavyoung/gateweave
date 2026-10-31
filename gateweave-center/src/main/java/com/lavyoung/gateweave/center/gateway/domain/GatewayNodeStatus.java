package com.lavyoung.gateweave.center.gateway.domain;

/**
 * 表示控制面维护的网关节点运行状态。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/31
 */
public enum GatewayNodeStatus {

    /**
     * 节点已经注册并可以参与配置同步。
     */
    ONLINE,

    /**
     * 节点已被注销或因心跳超时被判定为离线。
     */
    OFFLINE
}
