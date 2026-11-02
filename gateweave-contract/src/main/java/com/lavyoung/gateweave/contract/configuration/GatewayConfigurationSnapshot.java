package com.lavyoung.gateweave.contract.configuration;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * 描述控制面发布给网关数据面的不可变配置快照。
 *
 * <p>version 表示同一网关分组内单调递增的发布版本。checksum 是控制面对持久化原始快照内容
 * 计算的摘要，数据面将其作为不透明版本指纹使用，不得重新序列化 routes 后自行计算。
 * 快照只包含已经发布的配置，禁止使用尚未发布的候选配置构造该契约。</p>
 *
 * @param gatewayGroupId 快照所属网关分组标识
 * @param version        网关分组内的发布版本
 * @param publishedAt    快照发布时间
 * @param checksum       控制面持久化原始快照内容的摘要
 * @param routes         快照包含的不可变路由配置
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/02
 */
public record GatewayConfigurationSnapshot(String gatewayGroupId, long version, Instant publishedAt,
                                           String checksum, List<RouteConfiguration> routes) {
    /**
     * 校验快照基础属性并复制路由集合。
     *
     * @throws NullPointerException     gatewayGroupId、publishedAt、checksum、routes 为 null，
     *                                  或 routes 包含 null 元素时抛出
     * @throws IllegalArgumentException gatewayGroupId 或 checksum 为空白，或者 version 小于 1 时抛出
     */
    public GatewayConfigurationSnapshot {
        Objects.requireNonNull(gatewayGroupId, "gatewayGroupId must not be null");
        Objects.requireNonNull(publishedAt, "publishedAt must not be null");
        Objects.requireNonNull(checksum, "checksum must not be null");
        Objects.requireNonNull(routes, "routes must not be null");

        if (gatewayGroupId.isBlank()) {
            throw new IllegalArgumentException("gatewayGroupId must not be blank");
        }
        if (version < 1) {
            throw new IllegalArgumentException("version must be greater than zero");
        }
        if (checksum.isBlank()) {
            throw new IllegalArgumentException("checksum must not be blank");
        }

        routes = List.copyOf(routes);
    }
}
