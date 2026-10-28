package com.lavyoung.gateweave.engine.domain.invocation;

import java.util.Objects;

/**
 * 描述目标方法中一个具有固定位置的调用参数。
 *
 * <p>参数名称用于绑定入口请求值，参数类型用于生成协议插件需要的方法签名。</p>
 *
 * @param name     HTTP 请求中的参数名称
 * @param typeName 目标方法参数类型的全限定名
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/28
 */
public record InvocationParameter(String name, String typeName) {

    /**
     * 校验参数名称和参数类型。
     *
     * @throws NullPointerException     name 或 typeName 为 null 时抛出
     * @throws IllegalArgumentException name 或 typeName 为空白时抛出
     */
    public InvocationParameter {
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
