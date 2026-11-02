package com.lavyoung.gateweave.center.release.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 验证路由候选发布和 Engine 快照查询的完整控制面链路。
 *
 * <p>测试使用 Flyway 初始化的真实 H2 数据库，不替换应用用例、候选查询或发布 Repository。
 * 发布后直接修改编辑表，用于证明历史快照内容不会随候选配置发生变化。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/02
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:configuration-release-endpoint;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@AutoConfigureMockMvc
@Transactional
class ConfigurationReleaseEndpointIntegrationTests {

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
    private static final String INSERT_ROUTE = """
            INSERT INTO gw_route
                (route_id, gateway_group_id, path, http_method, service_operation_id,
                 authentication_mode, invocation_timeout_ms, target_attributes, route_attributes, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final MockMvc mockMvc;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    ConfigurationReleaseEndpointIntegrationTests(MockMvc mockMvc, JdbcTemplate jdbcTemplate) {
        this.mockMvc = mockMvc;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 验证发布操作会固化当前候选配置，后续编辑不会改变已经发布的快照。
     *
     * @throws Exception HTTP 请求执行失败时抛出
     */
    @Test
    void shouldPublishAndReadImmutableSnapshotThroughCompleteControlPlaneChain() throws Exception {
        // Given
        insertRouteCandidate();

        // When & Then
        this.mockMvc.perform(post("/api/v1/gateway-groups/default/configuration-releases"))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.gatewayGroupId").value("default"))
                .andExpect(jsonPath("$.data.version").value(1))
                .andExpect(jsonPath("$.data.checksum").value(org.hamcrest.Matchers.matchesPattern(
                        "sha256:[0-9a-f]{64}")))
                .andExpect(jsonPath("$.data.routes[0].routeId").value("route-query"))
                .andExpect(jsonPath("$.data.routes[0].path").value("/activities/query"));

        this.jdbcTemplate.update(
                "UPDATE gw_route SET path = '/activities/changed' WHERE route_id = 'route-query'");

        this.mockMvc.perform(get("/api/v1/gateway-groups/default/configuration-releases/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(1))
                .andExpect(jsonPath("$.data.routes[0].path").value("/activities/query"));
    }

    /**
     * 验证没有发布记录时 Engine 查询接口返回 HTTP 204。
     *
     * @throws Exception HTTP 请求执行失败时抛出
     */
    @Test
    void shouldReturnNoContentBeforeFirstConfigurationPublication() throws Exception {
        // When & Then
        this.mockMvc.perform(get("/api/v1/gateway-groups/default/configuration-releases/latest"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    private void insertRouteCandidate() {
        this.jdbcTemplate.update(
                INSERT_SERVICE, "activity-service", "activity-application", "dubbo", "", "1.0.0", "ACTIVE");
        this.jdbcTemplate.update(INSERT_OPERATION, "activity-query", "activity-service",
                "cn.example.ActivityService", "query", "ACTIVE");
        this.jdbcTemplate.update(
                INSERT_ROUTE, "route-query", "default", "/activities/query", "GET", "activity-query",
                "REQUIRED", 3000L, "{\"version\":\"1.0.0\"}", "{\"source\":\"center\"}", "ENABLED");
    }
}
