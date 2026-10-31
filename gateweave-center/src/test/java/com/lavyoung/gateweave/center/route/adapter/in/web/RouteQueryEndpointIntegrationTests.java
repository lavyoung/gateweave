package com.lavyoung.gateweave.center.route.adapter.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 验证路由分页查询从 HTTP 入口到数据库适配器的完整调用链。
 *
 * <p>测试使用 Flyway 初始化的 H2 数据库，不替换应用用例或 Repository，确保分页、聚合恢复和响应转换能够协同工作。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/30
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:gateweave-route-endpoint;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@AutoConfigureMockMvc
@Transactional
class RouteQueryEndpointIntegrationTests {

    private static final String INSERT_SERVICE = """
            INSERT INTO gw_service
                (service_id, application_name, protocol, namespace, service_version, status)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
    private static final String INSERT_OPERATION = """
            INSERT INTO gw_service_operation
                (operation_id, service_id, interface_name, operation_name, status)
            VALUES (?, ?, ?, ?, ?)
            """;
    private static final String INSERT_OPERATION_PARAMETER = """
            INSERT INTO gw_service_operation_parameter
                (operation_id, parameter_index, parameter_name, type_name)
            VALUES (?, ?, ?, ?)
            """;
    private static final String INSERT_ROUTE = """
            INSERT INTO gw_route
                (route_id, gateway_group_id, path, http_method, service_operation_id,
                 authentication_mode, invocation_timeout_ms, target_attributes, route_attributes, status, revision)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
    private static final String INSERT_ROUTE_PARAMETER = """
            INSERT INTO gw_route_parameter
                (route_id, service_operation_id, parameter_index, parameter_name)
            VALUES (?, ?, ?, ?)
            """;

    private final MockMvc mockMvc;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    RouteQueryEndpointIntegrationTests(MockMvc mockMvc, JdbcTemplate jdbcTemplate) {
        this.mockMvc = mockMvc;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 验证指定分页参数能够穿过全部分层，并返回由数据库恢复的完整路由聚合。
     *
     * @throws Exception HTTP 请求执行失败时抛出
     */
    @Test
    void shouldQueryPersistedRouteThroughCompleteApplicationChain() throws Exception {
        // Given
        this.insertServiceCatalog();
        this.insertRoute("route-one", "/activities/one", "{}", "{}");
        this.insertRoute("route-two", "/activities/two", "{\"version\":\"1.0.0\"}", "{\"tag\":\"stable\"}");
        this.insertRouteParameter("route-two", 0, "userName");
        this.insertRouteParameter("route-two", 1, "userAge");

        // When & Then
        this.mockMvc.perform(get("/api/v1/gateway-groups/default/routes")
                        .queryParam("pageNumber", "2")
                        .queryParam("pageSize", "1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.pageNumber").value(2))
                .andExpect(jsonPath("$.data.pageSize").value(1))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.routes.length()").value(1))
                .andExpect(jsonPath("$.data.routes[0].routeId").value("route-two"))
                .andExpect(jsonPath("$.data.routes[0].path").value("/activities/two"))
                .andExpect(jsonPath("$.data.routes[0].targetAttributes.version").value("1.0.0"))
                .andExpect(jsonPath("$.data.routes[0].attributes.tag").value("stable"))
                .andExpect(jsonPath("$.data.routes[0].parameterMappings[0].position").value(0))
                .andExpect(jsonPath("$.data.routes[0].parameterMappings[0].requestName").value("userName"))
                .andExpect(jsonPath("$.data.routes[0].parameterMappings[1].position").value(1))
                .andExpect(jsonPath("$.data.routes[0].parameterMappings[1].requestName").value("userAge"));
    }

    /**
     * 写入路由外键依赖的服务、操作和参数元数据。
     */
    private void insertServiceCatalog() {
        this.jdbcTemplate.update(
                INSERT_SERVICE, "activity-service", "activity-application", "dubbo", "", "1.0.0", "ACTIVE");
        this.jdbcTemplate.update(INSERT_OPERATION, "activity-query", "activity-service",
                "cn.example.ActivityService", "query", "ACTIVE");
        this.jdbcTemplate.update(INSERT_OPERATION_PARAMETER, "activity-query", 0, "name", "java.lang.String");
        this.jdbcTemplate.update(INSERT_OPERATION_PARAMETER, "activity-query", 1, "age", "java.lang.Integer");
    }

    /**
     * 写入一条路由。
     *
     * @param routeId         路由标识
     * @param path            HTTP 路径
     * @param targetAttributes 目标调用扩展属性 JSON
     * @param routeAttributes 路由扩展属性 JSON
     */
    private void insertRoute(String routeId, String path, String targetAttributes, String routeAttributes) {
        this.jdbcTemplate.update(INSERT_ROUTE, routeId, "default", path, "GET", "activity-query", "REQUIRED",
                3000L, targetAttributes, routeAttributes, "ENABLED", 2L);
    }

    /**
     * 写入一条路由参数映射。
     *
     * @param routeId    路由标识
     * @param position   目标服务参数位置
     * @param requestName HTTP 请求参数名
     */
    private void insertRouteParameter(String routeId, int position, String requestName) {
        this.jdbcTemplate.update(INSERT_ROUTE_PARAMETER, routeId, "activity-query", position, requestName);
    }
}
