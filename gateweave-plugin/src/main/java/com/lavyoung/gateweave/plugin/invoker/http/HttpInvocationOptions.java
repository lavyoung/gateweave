package com.lavyoung.gateweave.plugin.invoker.http;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * 描述 HTTP 下游调用需要的协议专属配置。
 *
 * <p>该类型负责把调用契约中的扩展属性转换为有明确语义的 HTTP 配置，
 * 避免 HTTP 调用器直接读取无类型的属性 Map。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/27
 */
public record HttpInvocationOptions(String method) {

    private static final String METHOD_ATTRIBUTE = "method";

    /**
     * 校验并规范化 HTTP 调用配置。
     *
     * @throws NullPointerException     method 为 null 时抛出
     * @throws IllegalArgumentException method 为空白时抛出
     */
    public HttpInvocationOptions {
        Objects.requireNonNull(method, "method must not be null");
        if (method.isBlank()) {
            throw new IllegalArgumentException("method must not be blank");
        }
        method = method.toUpperCase(Locale.ROOT);
    }

    /**
     * 从调用目标扩展属性中解析 HTTP 配置。
     *
     * @param attributes 调用目标扩展属性
     * @return 完成校验和规范化的 HTTP 调用配置
     * @throws NullPointerException     attributes 或 method 为 null 时抛出
     * @throws IllegalArgumentException method 为空白时抛出
     */
    public static HttpInvocationOptions from(Map<String, String> attributes) {
        Objects.requireNonNull(attributes, "attributes must not be null");
        return new HttpInvocationOptions(attributes.get(METHOD_ATTRIBUTE));
    }
}
