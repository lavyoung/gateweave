package com.lavyoung.gateweave.center.gateway.application;

import java.util.UUID;

/**
 * 定义网关节点主动释放租约的入站用例。
 *
 * <p>网关正常关闭时通过该用例主动下线当前运行实例，避免必须等待租约超时扫描。
 * 重复释放、节点不存在或租约已经失效均按幂等成功处理。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/11
 */
@FunctionalInterface
public interface ReleaseGatewayNodeLeaseUseCase {

    /**
     * 使用当前租约主动下线网关节点。
     *
     * @param nodeId  节点唯一标识
     * @param leaseId 当前运行实例持有的租约标识
     * @throws NullPointerException     nodeId 或 leaseId 为 null 时抛出
     * @throws IllegalArgumentException nodeId 为空白时抛出
     */
    void release(String nodeId, UUID leaseId);
}
