package com.lavyoung.gateweave.center.release.application;


import com.lavyoung.gateweave.contract.configuration.GatewayConfigurationSnapshot;

/**
 * 定义将网关分组候选配置发布为不可变快照的应用用例。
 *
 * <p>发布过程需要聚合当前候选路由、完成完整性校验、分配新版本并持久化快照。
 * 已发布快照不得因后续候选配置变化而改变。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/02
 */
public interface PublishGatewayConfigurationUseCase {

    /**
     * 发布指定网关分组的当前候选配置。
     *
     * @param gatewayGroupId 网关分组标识
     * @return 本次生成并持久化的不可变配置快照
     * @throws NullPointerException     gatewayGroupId 为 null 时抛出
     * @throws IllegalArgumentException gatewayGroupId 为空白时抛出
     */
    GatewayConfigurationSnapshot publish(String gatewayGroupId);
}
