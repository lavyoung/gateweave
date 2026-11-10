package com.lavyoung.gateweave.center.gateway.application;

/**
 * 定义网关节点过期租约回收用例。
 *
 * <p>控制面调度适配器通过该用例触发过期节点扫描，不得直接访问 Repository，
 * 也不得自行计算心跳截止时间。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/09
 */
@FunctionalInterface
public interface ExpireGatewayNodeLeasesUseCase {

    /**
     * 根据当前控制面时间和租约超时时间，将过期在线节点标记为离线。
     *
     * @return 本次被标记为离线的节点数量
     */
    int expire();
}
