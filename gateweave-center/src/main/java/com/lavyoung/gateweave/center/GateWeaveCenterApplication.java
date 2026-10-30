package com.lavyoung.gateweave.center;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * GateWeave 控制面进程入口。
 *
 * <p>该进程负责服务目录、路由配置、版本发布、网关节点和管理 API。
 * 启动类位于 Center 根包，确保后续各层组件均处于 Spring 默认扫描范围内。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/30
 */
@SpringBootApplication(proxyBeanMethods = false)
public class GateWeaveCenterApplication {

    /**
     * 启动 GateWeave 控制面进程。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(GateWeaveCenterApplication.class, args);
    }
}
