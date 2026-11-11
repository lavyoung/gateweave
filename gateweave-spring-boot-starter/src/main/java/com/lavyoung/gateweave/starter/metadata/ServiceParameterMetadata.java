package com.lavyoung.gateweave.starter.metadata;

import java.util.Objects;

/**
 * 描述 Starter 从服务方法签名中采集的有序参数。
 *
 * @param position 参数在方法签名中的位置，从零开始
 * @param name     参数名称
 * @param typeName 参数类型全限定名
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/11
 */
public record ServiceParameterMetadata(int position, String name, String typeName) {

    /**
     * 校验并规范化服务参数元数据。
     *
     * @throws NullPointerException     name 或 typeName 为 null 时抛出
     * @throws IllegalArgumentException position 为负数或文本为空白时抛出
     */
    public ServiceParameterMetadata {
        name = Objects.requireNonNull(name, "name must not be null").strip();
        typeName = Objects.requireNonNull(typeName, "typeName must not be null").strip();

        if (position < 0) {
            throw new IllegalArgumentException("position must not be negative");
        }
        if (name.isEmpty()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (typeName.isEmpty()) {
            throw new IllegalArgumentException("typeName must not be blank");
        }
    }
}
