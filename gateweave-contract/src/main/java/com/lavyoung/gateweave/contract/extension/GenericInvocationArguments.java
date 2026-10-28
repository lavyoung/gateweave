package com.lavyoung.gateweave.contract.extension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 描述一次泛化调用的方法参数类型和参数值。
 *
 * <p>参数类型与参数值按照方法声明顺序一一对应。该契约只使用 JDK 类型，
 * 不暴露 Dubbo GenericService 等厂商 SDK 类型。</p>
 *
 * @param parameterTypes RPC 方法参数类型的全限定名列表
 * @param arguments      按方法声明顺序排列的参数值列表
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/28
 */
public record GenericInvocationArguments(List<String> parameterTypes, List<Object> arguments) {

    /**
     * 校验参数类型与参数值，并建立不可变列表快照。
     *
     * @throws NullPointerException     参数类型列表或参数值列表为 null 时抛出
     * @throws IllegalArgumentException 参数类型为空白，或类型数量与参数数量不一致时抛出
     */
    public GenericInvocationArguments {
        Objects.requireNonNull(parameterTypes, "parameterTypes must not be null");
        Objects.requireNonNull(arguments, "arguments must not be null");

        if (parameterTypes.stream().anyMatch(parameterType -> parameterType == null || parameterType.isBlank())) {
            throw new IllegalArgumentException("parameterTypes must not contain null or blank values");
        }
        if (parameterTypes.size() != arguments.size()) {
            throw new IllegalArgumentException("parameterTypes and arguments must have the same size");
        }

        parameterTypes = List.copyOf(parameterTypes);
        // 允许传递 null 参数，而 List.copyOf()会拒绝包含 null 的集合。因此这里复用 JDK 的
        arguments = Collections.unmodifiableList(new ArrayList<>(arguments));
    }
}
