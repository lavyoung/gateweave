package com.lavyoung.gateweave.plugin.invoker.rpc;

import java.util.Map;
import java.util.Objects;

/**
 * 描述 Dubbo 服务引用需要的协议专属配置。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @since 1.0
 */
public record DubboReferenceOptions(String version) {

    private static final String VERSION_ATTRIBUTE = "version";

    /**
     * 校验 Dubbo 服务引用配置。
     *
     * @throws NullPointerException     version 为 null 时抛出
     * @throws IllegalArgumentException version 为空白时抛出
     */
    public DubboReferenceOptions {
        Objects.requireNonNull(version, "version must not be null");
        if (version.isBlank()) {
            throw new IllegalArgumentException("version must not be blank");
        }
    }

    /**
     * 从调用目标的扩展属性中解析 Dubbo 配置。
     *
     * @param attributes 调用目标扩展属性
     * @return 完成校验的 Dubbo 服务引用配置
     * @throws NullPointerException     attributes 或 version 为 null 时抛出
     * @throws IllegalArgumentException version 为空白时抛出
     */
    public static DubboReferenceOptions from(Map<String, String> attributes) {
        Objects.requireNonNull(attributes, "attributes must not be null");
        return new DubboReferenceOptions(attributes.get(VERSION_ATTRIBUTE));
    }
}
