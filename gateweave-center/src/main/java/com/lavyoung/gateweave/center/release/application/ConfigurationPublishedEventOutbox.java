package com.lavyoung.gateweave.center.release.application;

import com.lavyoung.gateweave.contract.configuration.GatewayConfigurationPublishedEvent;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 定义配置发布事件的可靠投递存储边界。
 *
 * <p>实现负责领取到期事件，并通过实例级工作者标识防止多个控制面节点重复领取。
 * 批量大小、锁定时长、重试间隔和最大尝试次数属于实现的投递策略。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/15
 */
public interface ConfigurationPublishedEventOutbox {

    /**
     * 原子领取当前已经到期的配置发布事件。
     *
     * @param claimedAt 本轮领取时间
     * @return 已由当前工作者锁定的事件，不存在时返回空列表
     * @throws NullPointerException claimedAt 为 null 时抛出
     */
    List<GatewayConfigurationPublishedEvent> claimDue(Instant claimedAt);

    /**
     * 将当前工作者持有的事件标记为投递成功。
     *
     * @param eventId     事件唯一标识
     * @param deliveredAt 投递完成时间
     * @throws NullPointerException eventId 或 deliveredAt 为 null 时抛出
     */
    void markPublished(UUID eventId, Instant deliveredAt);

    /**
     * 记录当前工作者持有事件的投递失败结果。
     *
     * <p>实现根据当前尝试次数决定重新进入待投递状态还是进入终止状态。</p>
     *
     * @param eventId      事件唯一标识
     * @param failedAt     本次失败时间
     * @param errorMessage 内部失败信息
     * @throws NullPointerException eventId、failedAt 或 errorMessage 为 null 时抛出
     */
    void markFailed(UUID eventId, Instant failedAt, String errorMessage);
}
