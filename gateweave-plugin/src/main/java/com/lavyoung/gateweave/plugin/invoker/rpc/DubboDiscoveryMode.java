package com.lavyoung.gateweave.plugin.invoker.rpc;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.registry.client.migration.model.MigrationStep;

/**
 * 定义 Dubbo 消费端使用的服务发现模式。
 *
 * <p>该枚举隔离 Dubbo 内部迁移参数，避免厂商类型越过插件边界。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @since 1.0
 */
public enum DubboDiscoveryMode {

    /**
     * 仅使用接口级服务发现，适用于 Dubbo 2.x 注册模型。
     */
    INTERFACE(MigrationStep.FORCE_INTERFACE, CommonConstants.INTERFACE_REGISTER_MODE),

    /**
     * 优先使用应用级服务发现，同时兼容接口级服务发现。
     */
    APPLICATION_FIRST(MigrationStep.APPLICATION_FIRST, CommonConstants.DEFAULT_REGISTER_MODE),

    /**
     * 仅使用应用级服务发现。
     */
    APPLICATION(MigrationStep.FORCE_APPLICATION, CommonConstants.INSTANCE_REGISTER_MODE);

    private final MigrationStep migrationStep;
    private final String registerMode;

    DubboDiscoveryMode(MigrationStep migrationStep, String registerMode) {
        this.migrationStep = migrationStep;
        this.registerMode = registerMode;
    }

    /**
     * 获取对应的 Dubbo 迁移步骤参数。
     *
     * @return Dubbo 迁移步骤
     */
    public String migrationStep() {
        return this.migrationStep.name();
    }

    /**
     * 获取对应的 Dubbo 注册模式。
     *
     * @return Dubbo 注册模式
     */
    public String registerMode() {
        return this.registerMode;
    }
}
