package com.lavyoung.gateweave.center.release.application;

import com.lavyoung.gateweave.contract.configuration.GatewayConfigurationSnapshot;

import java.util.Optional;

/**
 * 定义查询网关分组已发布配置快照的应用用例。
 *
 * <p>该用例只读取已经发布的不可变快照，不读取路由候选配置，
 * 供 Engine 启动拉取及后续配置同步使用。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/02
 */
public interface QueryPublishedGatewayConfigurationUseCase {

    /**
     * 查询指定网关分组最后一个已发布配置快照。
     *
     * @param gatewayGroupId 网关分组标识
     * @return 存在已发布版本时返回快照，否则返回空
     * @throws NullPointerException     gatewayGroupId 为 null 时抛出
     * @throws IllegalArgumentException gatewayGroupId 为空白时抛出
     */
    Optional<GatewayConfigurationSnapshot> findLatestPublished(String gatewayGroupId);
}
