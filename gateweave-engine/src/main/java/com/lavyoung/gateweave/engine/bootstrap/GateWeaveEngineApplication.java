package com.lavyoung.gateweave.engine.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.lavyoung.gateweave.engine.configuration.GatewayEngineConfiguration;

/**
 * GateWeave 网关数据面进程启动入口。
 *
 * <p>该类只负责启动 Spring Boot 并定位最终装配配置，不承载路由、协议调用或网络处理逻辑。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @since 1.0
 */
@SpringBootApplication(scanBasePackageClasses = GatewayEngineConfiguration.class, proxyBeanMethods = false)
public class GateWeaveEngineApplication {

    /**
     * 启动 GateWeave 网关数据面进程。
     *
     * @param args JVM 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(GateWeaveEngineApplication.class, args);
    }
}
