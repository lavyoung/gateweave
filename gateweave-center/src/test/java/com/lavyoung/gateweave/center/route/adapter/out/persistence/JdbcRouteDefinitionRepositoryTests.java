package com.lavyoung.gateweave.center.route.adapter.out.persistence;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.lavyoung.gateweave.center.GateWeaveCenterApplication;
import com.lavyoung.gateweave.center.route.domain.RouteDefinition;
import com.lavyoung.gateweave.center.route.domain.RouteParameterMapping;
import com.lavyoung.gateweave.center.route.domain.repository.RouteDefinitionRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JdbcRouteDefinitionRepositoryTests {

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

    /**
     * 验证分页查询能够批量装配多条路由各自的参数映射和扩展属性。
     */
    @Test
    void shouldCountAndLoadPagedRouteAggregates() {
        // Given
        WebApplicationContextRunner contextRunner = contextRunner("gateweave-route-repository-page");

        // When & Then
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);
            RouteDefinitionRepository repository = context.getBean(RouteDefinitionRepository.class);
            insertServiceCatalog(jdbcTemplate);
            insertRoute(jdbcTemplate, new RouteFixture(
                    "route-b", "/activities/b", "{\"version\":\"1.0.0\"}", "{\"tag\":\"beta\"}"));
            insertRoute(jdbcTemplate, new RouteFixture("route-a", "/activities/a", "{}", "{}"));
            insertParameters(jdbcTemplate, "route-b", List.of("userName", "userAge"));
            insertParameters(jdbcTemplate, "route-a", List.of("name", "age"));

            List<RouteDefinition> routes = repository.findPageByGatewayGroupId("default", 0, 10);

            assertThat(repository.countByGatewayGroupId("default")).isEqualTo(2);
            assertThat(routes).extracting(RouteDefinition::routeId).containsExactly("route-b", "route-a");
            assertThat(routes.get(0).invocationTimeout()).isEqualTo(Duration.ofSeconds(3));
            assertThat(routes.get(0).targetAttributes()).containsEntry("version", "1.0.0");
            assertThat(routes.get(0).attributes()).containsEntry("tag", "beta");
            assertThat(routes.get(0).parameterMappings())
                    .extracting(RouteParameterMapping::requestName)
                    .containsExactly("userName", "userAge");
            assertThat(routes.get(1).parameterMappings())
                    .extracting(RouteParameterMapping::requestName)
                    .containsExactly("name", "age");
        });
    }

    /**
     * 验证查询偏移量生效，并且没有数据时返回空集合。
     */
    @Test
    void shouldApplyOffsetAndReturnEmptyPage() {
        // Given
        WebApplicationContextRunner contextRunner = contextRunner("gateweave-route-repository-offset");

        // When & Then
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);
            RouteDefinitionRepository repository = context.getBean(RouteDefinitionRepository.class);
            insertServiceCatalog(jdbcTemplate);
            insertRoute(jdbcTemplate, new RouteFixture("route-first", "/first", "{}", "{}"));
            insertRoute(jdbcTemplate, new RouteFixture("route-second", "/second", "{}", "{}"));

            assertThat(repository.findPageByGatewayGroupId("default", 1, 1))
                    .extracting(RouteDefinition::routeId)
                    .containsExactly("route-second");
            assertThat(repository.findPageByGatewayGroupId("missing", 0, 10)).isEmpty();
            assertThat(repository.countByGatewayGroupId("missing")).isZero();
        });
    }

    /**
     * 验证分页参数在进入 SQL 前完成边界检查。
     */
    @Test
    void shouldRejectInvalidPageArguments() {
        // Given
        WebApplicationContextRunner contextRunner = contextRunner("gateweave-route-repository-validation");

        // When & Then
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            RouteDefinitionRepository repository = context.getBean(RouteDefinitionRepository.class);

            assertThatThrownBy(() -> repository.findPageByGatewayGroupId("default", -1, 10))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("offset must not be negative");
            assertThatThrownBy(() -> repository.findPageByGatewayGroupId("default", 0, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("limit must be greater than zero");
        });
    }

    /**
     * 验证损坏的数据库 JSON 被适配器转换为稳定的数据访问异常。
     */
    @Test
    void shouldRejectMalformedRouteAttributes() {
        // Given
        WebApplicationContextRunner contextRunner = contextRunner("gateweave-route-repository-json");

        // When & Then
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);
            RouteDefinitionRepository repository = context.getBean(RouteDefinitionRepository.class);
            insertServiceCatalog(jdbcTemplate);
            insertRoute(jdbcTemplate, new RouteFixture("route-broken", "/broken", "not-json", "{}"));

            assertThatThrownBy(() -> repository.findPageByGatewayGroupId("default", 0, 10))
                    .isInstanceOf(DataRetrievalFailureException.class)
                    .hasMessageContaining("target_attributes")
                    .hasMessageContaining("route-broken");
        });
    }

    /**
     * 建立路由外键依赖的服务、操作和参数元数据。
     *
     * @param jdbcTemplate JDBC 操作入口
     */
    private static void insertServiceCatalog(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.update(
                INSERT_SERVICE, "activity-service", "activity-application", "dubbo", "", "1.0.0", "ACTIVE");
        jdbcTemplate.update(INSERT_OPERATION, "activity-query", "activity-service",
                "cn.example.ActivityService", "query", "ACTIVE");
        jdbcTemplate.update(INSERT_OPERATION_PARAMETER, "activity-query", 0, "name", "java.lang.String");
        jdbcTemplate.update(INSERT_OPERATION_PARAMETER, "activity-query", 1, "age", "java.lang.Integer");
    }

    /**
     * 写入一条测试路由。
     *
     * @param jdbcTemplate JDBC 操作入口
     * @param fixture      路由测试数据
     */
    private static void insertRoute(JdbcTemplate jdbcTemplate, RouteFixture fixture) {
        jdbcTemplate.update(INSERT_ROUTE, fixture.routeId(), "default", fixture.path(), "GET", "activity-query",
                "REQUIRED", 3000L, fixture.targetAttributes(), fixture.routeAttributes(), "ENABLED", 2L);
    }

    /**
     * 写入指定路由的两个有序参数映射。
     *
     * @param jdbcTemplate JDBC 操作入口
     * @param routeId      路由标识
     * @param names        按目标位置排列的请求参数名
     */
    private static void insertParameters(JdbcTemplate jdbcTemplate, String routeId, List<String> names) {
        for (int position = 0; position < names.size(); position++) {
            jdbcTemplate.update(INSERT_ROUTE_PARAMETER, routeId, "activity-query", position, names.get(position));
        }
    }

    /**
     * 创建独立的 Spring Web 测试上下文和 H2 数据库。
     *
     * @param databaseName 测试数据库名称
     * @return 测试上下文运行器
     */
    private static WebApplicationContextRunner contextRunner(String databaseName) {
        return new WebApplicationContextRunner()
                .withUserConfiguration(GateWeaveCenterApplication.class)
                .withPropertyValues(
                        "spring.datasource.url=jdbc:h2:mem:%s;MODE=MySQL;DB_CLOSE_DELAY=0".formatted(databaseName),
                        "spring.datasource.username=sa",
                        "spring.datasource.password=");
    }

    /**
     * 保存单条路由测试数据。
     *
     * @param routeId          路由标识
     * @param path             HTTP 路径
     * @param targetAttributes 目标扩展属性 JSON
     * @param routeAttributes  路由扩展属性 JSON
     */
    private record RouteFixture(String routeId, String path, String targetAttributes, String routeAttributes) {
    }
}
