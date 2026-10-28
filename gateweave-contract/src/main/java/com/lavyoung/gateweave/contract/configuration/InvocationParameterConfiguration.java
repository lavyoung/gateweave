package com.lavyoung.gateweave.contract.configuration;

import java.util.Objects;

/**
 * 描述路由配置中的一个有序调用参数。
 *
 * <p>参数在列表中的位置对应目标方法的参数位置，参数名称用于从 HTTP 请求中查找输入值。</p>
 *
 * @param name     HTTP 请求中的参数名称
 * @param typeName 目标方法参数类型的全限定名
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/28
 */
public record InvocationParameterConfiguration(String name, String typeName) {

    /**
     * 校验参数名称和参数类型。
     *
     * @throws NullPointerException     name 或 typeName 为 null 时抛出
     * @throws IllegalArgumentException name 或 typeName 为空白时抛出
     */
    public InvocationParameterConfiguration {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(typeName, "typeName must not be null");

        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (typeName.isBlank()) {
            throw new IllegalArgumentException("typeName must not be blank");
        }
    }
}
