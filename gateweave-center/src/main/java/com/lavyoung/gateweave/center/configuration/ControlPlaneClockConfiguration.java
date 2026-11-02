package com.lavyoung.gateweave.center.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * 配置控制面各业务能力共享的统一时钟。
 *
 * <p>节点注册、配置发布及后续审计均使用 UTC 时间，避免不同能力分别创建时钟，
 * 同时允许测试环境替换为固定时钟。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/02
 */
@Configuration(proxyBeanMethods = false)
public class ControlPlaneClockConfiguration {

    /**
     * 提供控制面统一使用的 UTC 时钟。
     *
     * @return 系统 UTC 时钟
     */
    @Bean
    public Clock controlPlaneClock() {
        return Clock.systemUTC();
    }
}
