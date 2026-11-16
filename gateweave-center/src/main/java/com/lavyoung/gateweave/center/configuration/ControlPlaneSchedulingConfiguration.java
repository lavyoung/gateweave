package com.lavyoung.gateweave.center.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.TaskUtils;

/**
 * 集中装配控制面周期任务使用的调度资源。
 *
 * <p>节点租约维护和配置事件投递属于相互独立的阻塞任务，因此使用两个工作线程，
 * 避免 Redis 或数据库短暂阻塞导致另一条维护链路无法按时执行。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/16
 */
@Configuration(proxyBeanMethods = false)
public class ControlPlaneSchedulingConfiguration {

    /**
     * 创建控制面共享任务调度器。
     *
     * @return 控制面任务调度器
     */
    @Bean(name = "controlPlaneTaskScheduler", destroyMethod = "shutdown")
    public ThreadPoolTaskScheduler controlPlaneTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("gateweave-control-plane-");
        scheduler.setRemoveOnCancelPolicy(true);
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(5);
        scheduler.setErrorHandler(TaskUtils.LOG_AND_SUPPRESS_ERROR_HANDLER);
        return scheduler;
    }
}
