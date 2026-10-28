package com.lavyoung.gateweave.engine.application.command;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 保存 HTTP 查询参数或表单参数的不可变多值快照。
 *
 * <p>同一个参数名称可以对应多个值。没有内容的参数使用空字符串表示，
 * 例如 {@code name=} 对应包含一个空字符串的列表，而不是空列表。</p>
 *
 * @param values 按参数名称组织的多值参数
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/28
 */
public record HttpParameterValues(Map<String, List<String>> values) {

    /**
     * 校验参数名称和参数值，并建立两层不可变快照。
     *
     * @throws NullPointerException     values 为 null 时抛出
     * @throws IllegalArgumentException 参数名称为空白、值列表为空或包含 null 时抛出
     */
    public HttpParameterValues {
        Objects.requireNonNull(values, "values must not be null");

        for (Map.Entry<String, List<String>> entry : values.entrySet()) {
            String name = entry.getKey();
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("values must not contain null or blank names");
            }

            List<String> parameterValues = entry.getValue();
            if (parameterValues == null || parameterValues.isEmpty()) {
                throw new IllegalArgumentException("values must not contain null or empty value lists");
            }
            if (parameterValues.stream().anyMatch(Objects::isNull)) {
                throw new IllegalArgumentException("values must not contain null parameter values");
            }
        }

        values = values.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> List.copyOf(entry.getValue())));
    }
}
