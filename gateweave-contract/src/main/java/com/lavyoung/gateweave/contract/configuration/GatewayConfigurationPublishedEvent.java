package com.lavyoung.gateweave.contract.configuration;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * 表示某个网关分组已经发布新配置版本的跨进程事件。
 *
 * <p>该事件只用于提示数据面拉取最新配置，不携带完整路由快照。
 * Redis 等消息适配器只负责传输事件，配置内容仍通过控制面查询接口获取。</p>
 *
 * @param eventId        本次发布事件的唯一标识
 * @param gatewayGroupId 配置所属网关分组标识
 * @param version        网关分组内已经发布的配置版本
 * @param publishedAt    配置发布时间
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/15
 */
public record GatewayConfigurationPublishedEvent(UUID eventId, String gatewayGroupId, long version,
        Instant publishedAt) {

    /**
     * 校验配置发布事件的稳定身份和版本信息。
     *
     * @throws NullPointerException     eventId、gatewayGroupId 或 publishedAt 为 null 时抛出
     * @throws IllegalArgumentException gatewayGroupId 为空白，或者 version 小于 1 时抛出
     */
    public GatewayConfigurationPublishedEvent {
        eventId = Objects.requireNonNull(eventId, "eventId must not be null");
        gatewayGroupId = Objects.requireNonNull(gatewayGroupId, "gatewayGroupId must not be null").strip();
        publishedAt = Objects.requireNonNull(publishedAt, "publishedAt must not be null");

        if (gatewayGroupId.isEmpty()) {
            throw new IllegalArgumentException("gatewayGroupId must not be blank");
        }
        if (version < 1) {
            throw new IllegalArgumentException("version must be greater than zero");
        }
    }
}
