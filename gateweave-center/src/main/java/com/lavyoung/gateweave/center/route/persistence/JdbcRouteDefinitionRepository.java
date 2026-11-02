package com.lavyoung.gateweave.center.route.persistence;

import com.lavyoung.gateweave.center.route.domain.RouteAuthenticationRequirement;
import com.lavyoung.gateweave.center.route.domain.RouteDefinition;
import com.lavyoung.gateweave.center.route.domain.RouteDefinitionRepository;
import com.lavyoung.gateweave.center.route.domain.RouteParameterMapping;
import com.lavyoung.gateweave.center.route.domain.RouteStatus;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 使用 Spring JDBC 从关系数据库读取路由聚合。
 *
 * <p>适配器先分页读取路由主表，再使用一次批量查询读取当前页全部参数映射，
 * 最后把数据库记录转换为领域对象，避免逐条查询参数造成 N+1 问题。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/30
 */
@Repository
public class JdbcRouteDefinitionRepository implements RouteDefinitionRepository {

    private static final String COUNT_SQL = """
            SELECT COUNT(*)
            FROM gw_route
            WHERE gateway_group_id = :gatewayGroupId
            """;

    private static final String FIND_PAGE_SQL = """
            SELECT route_id,
                   gateway_group_id,
                   path,
                   http_method,
                   service_operation_id,
                   authentication_mode,
                   invocation_timeout_ms,
                   target_attributes,
                   route_attributes,
                   status,
                   revision
            FROM gw_route
            WHERE gateway_group_id = :gatewayGroupId
            ORDER BY id, route_id
            LIMIT :limit OFFSET :offset
            """;

    private static final String FIND_PARAMETERS_SQL = """
            SELECT route_id,
                   parameter_index,
                   parameter_name
            FROM gw_route_parameter
            WHERE route_id IN (:routeIds)
            ORDER BY route_id, parameter_index
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final RouteAttributesJsonReader attributesReader;

    /**
     * 创建路由持久化适配器。
     *
     * @param jdbcTemplate     支持命名参数和集合参数展开的 Spring JDBC 模板
     * @param attributesReader 路由属性 JSON 读取器
     * @throws NullPointerException 任一依赖为 null 时抛出
     */
    public JdbcRouteDefinitionRepository(
            NamedParameterJdbcTemplate jdbcTemplate, RouteAttributesJsonReader attributesReader) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
        this.attributesReader = Objects.requireNonNull(attributesReader, "attributesReader must not be null");
    }

    @Override
    public long countByGatewayGroupId(String gatewayGroupId) {
        Objects.requireNonNull(gatewayGroupId, "gatewayGroupId must not be null");
        MapSqlParameterSource parameter = new MapSqlParameterSource("gatewayGroupId", gatewayGroupId);
        Long count = this.jdbcTemplate.queryForObject(COUNT_SQL, parameter, Long.class);
        if (count == null) {
            throw new DataRetrievalFailureException("Route count query returned no result");
        }
        return count;
    }

    @Override
    public List<RouteDefinition> findPageByGatewayGroupId(String gatewayGroupId, long offset, int limit) {
        Objects.requireNonNull(gatewayGroupId, "gatewayGroupId must not be null");
        if (offset < 0) {
            throw new IllegalArgumentException("offset must not be negative");
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be greater than zero");
        }
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("gatewayGroupId", gatewayGroupId)
                .addValue("offset", offset)
                .addValue("limit", limit);
        List<RouteRow> routeRows = this.jdbcTemplate.query(FIND_PAGE_SQL, parameters, this::mapRouteRow);
        if (routeRows.isEmpty()) {
            return List.of();
        }
        List<String> routeIds = routeRows.stream().map(RouteRow::routeId).toList();
        Map<String, List<RouteParameterMapping>> mappingsByRouteId = this.findParameterMapping(routeIds);
        return routeRows.stream()
                .map(routeRow -> routeRow.toDomain(mappingsByRouteId.getOrDefault(routeRow.routeId(), List.of())))
                .toList();
    }


    /**
     * 批量读取当前页所有路由的参数映射。
     *
     * @param routeIds 当前页路由标识
     * @return 以路由标识分组的参数映射
     */
    private Map<String, List<RouteParameterMapping>> findParameterMapping(List<String> routeIds) {
        Map<String, List<RouteParameterMapping>> mappingsByRouteId = new LinkedHashMap<>();

        this.jdbcTemplate.query(FIND_PARAMETERS_SQL, Map.of("routeIds", routeIds), resultSet -> {
            String routeId = resultSet.getString("route_id");
            RouteParameterMapping parameterMapping = new RouteParameterMapping(
                    resultSet.getInt("parameter_index"), resultSet.getString("parameter_name"));
            mappingsByRouteId.computeIfAbsent(routeId, ignored -> new ArrayList<>()).add(parameterMapping);
        });

        return mappingsByRouteId;
    }

    /**
     * 将路由主表的一行转换为适配器内部记录。
     *
     * @param resultSet 当前结果集
     * @param rowNumber 当前行号
     * @return 尚未装配参数映射的路由记录
     * @throws SQLException 数据库字段读取失败时抛出
     */
    private RouteRow mapRouteRow(ResultSet resultSet, int rowNumber) throws SQLException {
        String routeId = resultSet.getString("route_id");
        try {
            return new RouteRow(routeId, resultSet.getString("gateway_group_id"), resultSet.getString("path"),
                    resultSet.getString("http_method"), resultSet.getString("service_operation_id"),
                    RouteAuthenticationRequirement.valueOf(resultSet.getString("authentication_mode")),
                    resultSet.getLong("invocation_timeout_ms"),
                    this.attributesReader.read(resultSet.getString("target_attributes"), routeId, "target_attributes"),
                    this.attributesReader.read(resultSet.getString("route_attributes"), routeId, "route_attributes"),
                    RouteStatus.valueOf(resultSet.getString("status")), resultSet.getLong("revision"));
        } catch (IllegalArgumentException exception) {
            throw new DataRetrievalFailureException("Route contains an unsupported enum value: " + routeId, exception);
        }
    }

    /**
     * 保存路由主表数据，等待参数映射批量加载完成后再创建完整领域对象。
     *
     * @param routeId                   路由标识
     * @param gatewayGroupId            网关分组标识
     * @param path                      HTTP 路径
     * @param method                    HTTP 方法
     * @param serviceOperationId        目标服务操作标识
     * @param authenticationRequirement 身份认证要求
     * @param invocationTimeoutMillis   调用超时毫秒数
     * @param targetAttributes          目标协议扩展属性
     * @param attributes                路由扩展属性
     * @param status                    路由状态
     * @param revision                  修订号
     */
    private record RouteRow(String routeId, String gatewayGroupId, String path, String method,
                            String serviceOperationId, RouteAuthenticationRequirement authenticationRequirement,
                            long invocationTimeoutMillis, Map<String, String> targetAttributes,
                            Map<String, String> attributes,
                            RouteStatus status, long revision) {

        /**
         * 将数据库记录和参数映射组装为完整路由聚合。
         *
         * @param parameterMappings 参数映射
         * @return 完整路由定义
         */
        private RouteDefinition toDomain(List<RouteParameterMapping> parameterMappings) {
            return new RouteDefinition(
                    this.routeId, this.gatewayGroupId, this.path, this.method, this.serviceOperationId,
                    this.authenticationRequirement, Duration.ofMillis(this.invocationTimeoutMillis),
                    this.targetAttributes, this.attributes, this.status, this.revision, parameterMappings);
        }
    }
}
