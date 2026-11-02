package com.lavyoung.gateweave.center.route.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

/**
 * 读取路由持久化表中的字符串属性 JSON。
 *
 * <p>该组件只处理路由表 JSON 字段到不可变字符串 Map 的防腐转换，
 * 不包含路由领域规则，也不作为跨模块公共工具暴露。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/02
 */
@Component
public final class RouteAttributesJsonReader {

    private static final TypeReference<Map<String, String>> STRING_MAP_TYPE = new TypeReference<Map<String, String>>() {
    };

    private final ObjectMapper objectMapper;

    /**
     * 创建路由属性 JSON 读取器。
     *
     * @param objectMapper 项目统一的 Jackson 对象映射器
     * @throws NullPointerException objectMapper 为 null 时抛出
     */
    RouteAttributesJsonReader(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    /**
     * 将数据库 JSON 字段读取为不可变字符串属性。
     *
     * @param json       数据库中的 JSON 文本
     * @param routeId    JSON 所属路由标识
     * @param columnName JSON 所属字段名称
     * @return 不可变字符串属性
     * @throws DataRetrievalFailureException JSON 无法解析、结果为 null 或包含 null 元素时抛出
     */
    Map<String, String> read(String json, String routeId, String columnName) {
        try {
            Map<String, String> attributes = this.objectMapper.readValue(json, STRING_MAP_TYPE);
            return Map.copyOf(Objects.requireNonNull(attributes, "attributes must not be null"));
        } catch (JsonProcessingException | NullPointerException exception) {
            String message = "Unable to read %s for route %s".formatted(columnName, routeId);
            throw new DataRetrievalFailureException(message, exception);
        }
    }
}
