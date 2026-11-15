package com.lavyoung.gateweave.center.release.application;

import com.lavyoung.gateweave.contract.configuration.GatewayConfigurationPublishedEvent;

/**
 * 定义配置发布通知的消息传输边界。
 *
 * <p>应用层只发布稳定事件，不感知 Redis Topic、序列化器或客户端连接。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/15
 */
public interface ConfigurationPublishedEventPublisher {

    /**
     * 向数据面节点发布配置变更提示。
     *
     * @param event 配置发布事件
     * @throws NullPointerException event 为 null 时抛出
     * @throws RuntimeException     消息传输失败时抛出
     */
    void publish(GatewayConfigurationPublishedEvent event);
}
