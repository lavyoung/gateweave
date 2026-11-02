package com.lavyoung.gateweave.center.release.application;

import com.lavyoung.gateweave.contract.configuration.GatewayConfigurationSnapshot;
import com.lavyoung.gateweave.contract.configuration.RouteConfiguration;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 定义配置快照发布和读取的持久化端口。
 *
 * <p>实现负责在同一事务中分配网关分组内的下一个版本、保存不可变快照并返回最终发布结果。
 * 快照摘要必须根据实际持久化的配置内容计算，避免摘要和存储内容不一致。</p>
 *
 * <p>该端口只暴露稳定配置契约，不暴露 JDBC、JSON、数据库实体或 Spring DAO 异常。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/02
 */
public interface ConfigurationReleaseRepository {

    /**
     * 将指定网关分组的路由候选发布为一个新版本。
     *
     * @param gatewayGroupId 网关分组标识
     * @param routes         已完成聚合的路由候选
     * @param publishedAt    本次发布时间
     * @return 已分配版本并持久化的配置快照
     * @throws NullPointerException     任一参数为 null，或 routes 包含 null 元素时抛出
     * @throws IllegalArgumentException gatewayGroupId 为空白时抛出
     */
    GatewayConfigurationSnapshot publish(String gatewayGroupId, List<RouteConfiguration> routes, Instant publishedAt);


    /**
     * 查询指定网关分组最后一个已发布快照。
     *
     * @param gatewayGroupId 网关分组标识
     * @return 存在已发布版本时返回快照，否则返回空
     * @throws NullPointerException     gatewayGroupId 为 null 时抛出
     * @throws IllegalArgumentException gatewayGroupId 为空白时抛出
     */
    Optional<GatewayConfigurationSnapshot> findLatestPublished(String gatewayGroupId);

}
