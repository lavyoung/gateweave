package com.lavyoung.gateweave.center.gateway.application;

import com.lavyoung.gateweave.center.shared.exception.BusinessException;

import java.util.UUID;

/**
 * 定义网关算力节点续租的入站用例。
 *
 * <p>HTTP Controller 及其他入站适配器通过该端口提交节点心跳，
 * 不得直接操作节点 Repository 或自行决定心跳接收时间。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/09
 */
@FunctionalInterface
public interface RenewGatewayNodeLeaseUseCase {

    /**
     * 使用当前租约续期节点，并报告节点已经加载的配置版本。
     *
     * @param nodeId               节点唯一标识
     * @param leaseId              当前运行实例持有的租约标识
     * @param currentConfigVersion 节点已经加载的配置版本
     * @throws NullPointerException     nodeId 或 leaseId 为 null 时抛出
     * @throws IllegalArgumentException nodeId 为空白或配置版本为负数时抛出
     * @throws BusinessException        节点租约已经失效时抛出
     */
    void renew(String nodeId, UUID leaseId, long currentConfigVersion);
}
