package com.lavyoung.gateweave.center.gateway.domain;

/**
 * 表示节点幂等注册后的实际处理结果。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/31
 */
public enum GatewayNodeRegistrationOutcome {

    /**
     * 数据库中原本不存在该节点，本次创建了新节点。
     */
    REGISTERED,

    /**
     * 数据库中已经存在该节点，本次刷新了节点信息和在线状态。
     */
    REFRESHED
}
