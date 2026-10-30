package com.lavyoung.gateweave.center.catalog.domain;

import java.util.Objects;

/**
 * 描述服务操作签名中的一个有序参数。
 *
 * <p>该值对象属于控制面的服务目录领域，记录服务原始方法签名。
 * 它不表达 HTTP 请求参数名称，也不依赖发布契约或数据面领域模型。</p>
 *
 * @param position 参数在服务操作签名中的位置，从 0 开始
 * @param name     服务操作声明的参数名称
 * @param typeName 参数类型的全限定名
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/30
 */
public record ServiceOperationParameter(int position, String name, String typeName) {

    /**
     * 校验服务操作参数的不变量。
     *
     * @throws NullPointerException     name 或 typeName 为 null 时抛出
     * @throws IllegalArgumentException position 为负数，或文本属性为空白时抛出
     */
    public ServiceOperationParameter {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(typeName, "typeName must not be null");

        if (position < 0) {
            throw new IllegalArgumentException("position must not be negative");
        }
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (typeName.isBlank()) {
            throw new IllegalArgumentException("typeName must not be blank");
        }
    }
}
