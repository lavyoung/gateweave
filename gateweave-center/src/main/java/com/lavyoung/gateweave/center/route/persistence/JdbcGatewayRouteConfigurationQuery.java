package com.lavyoung.gateweave.center.route.persistence;

import com.lavyoung.gateweave.center.route.application.GatewayRouteConfigurationQuery;
import com.lavyoung.gateweave.contract.configuration.InvocationParameterConfiguration;
import com.lavyoung.gateweave.contract.configuration.InvocationTargetConfiguration;
import com.lavyoung.gateweave.contract.configuration.RouteAuthenticationMode;
import com.lavyoung.gateweave.contract.configuration.RouteConfiguration;
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
 * 使用关系数据库生成网关分组的路由配置候选集。
 *
 * <p>适配器先聚合路由、服务和操作，再批量读取全部参数映射。
 * 查询结果只用于配置校验和后续发布，不代表已经发布的运行时快照。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/02
 */
@Repository
public class JdbcGatewayRouteConfigurationQuery implements GatewayRouteConfigurationQuery {

    private static final String FIND_CANDIDATE_ROUTES_SQL = """
            SELECT route.route_id,
                   route.path,
                   route.http_method,
                   route.authentication_mode,
                   route.invocation_timeout_ms,
                   route.target_attributes,
                   route.route_attributes,
                   service.service_id,
                   service.protocol,
                   service_operation.interface_name,
                   service_operation.operation_name
            FROM gw_route route
            INNER JOIN gw_service_operation service_operation
                    ON service_operation.operation_id = route.service_operation_id
            INNER JOIN gw_service service
                    ON service.service_id = service_operation.service_id
            WHERE route.gateway_group_id = :gatewayGroupId
              AND route.status = 'ENABLED'
              AND service.status = 'ACTIVE'
              AND service_operation.status = 'ACTIVE'
            ORDER BY route.id, route.route_id
            """;

    private static final String FIND_PARAMETERS_SQL = """
            SELECT route_parameter.route_id,
                   route_parameter.parameter_index,
                   route_parameter.parameter_name,
                   operation_parameter.type_name
            FROM gw_route_parameter route_parameter
            INNER JOIN gw_service_operation_parameter operation_parameter
                    ON operation_parameter.operation_id = route_parameter.service_operation_id
                   AND operation_parameter.parameter_index = route_parameter.parameter_index
            WHERE route_parameter.route_id IN (:routeIds)
            ORDER BY route_parameter.route_id, route_parameter.parameter_index
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final RouteAttributesJsonReader attributesReader;

    /**
     * 创建网关路由配置查询适配器。
     *
     * @param jdbcTemplate     支持命名参数和集合参数展开的 JDBC 模板
     * @param attributesReader 路由属性 JSON 读取器
     * @throws NullPointerException 任一依赖为 null 时抛出
     */
    public JdbcGatewayRouteConfigurationQuery(
            NamedParameterJdbcTemplate jdbcTemplate, RouteAttributesJsonReader attributesReader) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
        this.attributesReader = Objects.requireNonNull(attributesReader, "attributesReader must not be null");
    }

    @Override
    public List<RouteConfiguration> findCandidates(String gatewayGroupId) {
        String groupId = Objects.requireNonNull(gatewayGroupId, "gatewayGroupId must not be null").strip();
        if (groupId.isEmpty()) {
            throw new IllegalArgumentException("gatewayGroupId must not be blank");
        }
        MapSqlParameterSource parameters = new MapSqlParameterSource("gatewayGroupId", groupId);
        List<RouteProjectionRow> rows = this.jdbcTemplate.query(
                FIND_CANDIDATE_ROUTES_SQL, parameters, this::mapRouteRow);
        if (rows.isEmpty()) {
            return List.of();
        }
        List<String> routeIds = rows.stream().map(RouteProjectionRow::routeId).toList();
        Map<String, List<InvocationParameterConfiguration>> parametersByRouteId =
                this.findParametersByRouteId(routeIds);
        return rows.stream()
                .map(routeRow -> routeRow.toConfiguration(
                        parametersByRouteId.getOrDefault(routeRow.routeId(), List.of())))
                .toList();
    }

    /**
     * 批量加载候选路由的请求参数名称和目标参数类型。
     *
     * @param routeIds 候选路由标识
     * @return 按路由标识分组的有序调用参数
     */
    private Map<String, List<InvocationParameterConfiguration>> findParametersByRouteId(List<String> routeIds) {
        Map<String, List<InvocationParameterConfiguration>> parametersByRouteId = new LinkedHashMap<>();
        this.jdbcTemplate.query(FIND_PARAMETERS_SQL, Map.of("routeIds", routeIds), resultSet -> {
            String routeId = resultSet.getString("route_id");
            InvocationParameterConfiguration parameter = new InvocationParameterConfiguration(
                    resultSet.getString("parameter_name"), resultSet.getString("type_name"));
            parametersByRouteId.computeIfAbsent(routeId, ignored -> new ArrayList<>()).add(parameter);
        });
        return parametersByRouteId;
    }

    /**
     * 将主查询的一行转换为尚未装配参数的路由投影。
     *
     * @param resultSet 当前结果集
     * @param rowNumber 当前行号
     * @return 路由配置投影
     * @throws SQLException                  数据库字段读取失败时抛出
     * @throws DataRetrievalFailureException 数据库存储了不支持的认证模式时抛出
     */
    private RouteProjectionRow mapRouteRow(ResultSet resultSet, int rowNumber) throws SQLException {
        String routeId = resultSet.getString("route_id");
        try {
            return new RouteProjectionRow(
                    routeId,
                    resultSet.getString("path"),
                    resultSet.getString("http_method"),
                    RouteAuthenticationMode.valueOf(resultSet.getString("authentication_mode")),
                    resultSet.getLong("invocation_timeout_ms"),
                    this.attributesReader.read(
                            resultSet.getString("target_attributes"), routeId, "target_attributes"),
                    this.attributesReader.read(
                            resultSet.getString("route_attributes"), routeId, "route_attributes"),
                    resultSet.getString("service_id"),
                    resultSet.getString("protocol"),
                    resultSet.getString("interface_name"),
                    resultSet.getString("operation_name"));
        } catch (IllegalArgumentException exception) {
            throw new DataRetrievalFailureException(
                    "Route contains an unsupported authentication mode: " + routeId, exception);
        }
    }

    /**
     * 保存主查询结果，等待参数批量加载后再创建稳定配置契约。
     *
     * @param routeId                 路由标识
     * @param path                    HTTP 路径
     * @param method                  HTTP 方法
     * @param authenticationMode      路由认证模式
     * @param invocationTimeoutMillis 调用超时毫秒数
     * @param targetAttributes        协议调用属性
     * @param routeAttributes         路由扩展属性
     * @param serviceId               稳定服务标识
     * @param protocol                调用协议
     * @param interfaceName           目标服务接口名称
     * @param operationName           目标操作名称
     */
    private record RouteProjectionRow(
            String routeId, String path, String method, RouteAuthenticationMode authenticationMode,
            long invocationTimeoutMillis, Map<String, String> targetAttributes,
            Map<String, String> routeAttributes, String serviceId, String protocol,
            String interfaceName, String operationName) {

        /**
         * 将路由投影和有序参数组装为稳定配置契约。
         *
         * @param parameters 有序调用参数
         * @return 路由配置候选项
         */
        private RouteConfiguration toConfiguration(List<InvocationParameterConfiguration> parameters) {
            InvocationTargetConfiguration target = new InvocationTargetConfiguration(
                    this.protocol,
                    this.interfaceName,
                    this.operationName,
                    parameters,
                    Duration.ofMillis(this.invocationTimeoutMillis),
                    this.targetAttributes);
            return new RouteConfiguration(
                    this.routeId,
                    this.path,
                    this.method,
                    this.serviceId,
                    this.authenticationMode,
                    target,
                    this.routeAttributes);
        }
    }
}
