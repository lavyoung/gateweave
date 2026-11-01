package com.lavyoung.gateweave.center.catalog.adapter.in.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.lavyoung.gateweave.center.adapter.in.web.RequestIdFilter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 验证服务定义注册从 HTTP 入口到 JDBC 持久化适配器的完整调用链。
 *
 * <p>测试使用 Flyway 初始化的独立 H2 数据库，不替换 Controller、应用服务或 Repository，
 * 用于证明注册结果、幂等语义和定义冲突能够跨越全部分层保持一致。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/01
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:service-registration-endpoint;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@AutoConfigureMockMvc
class ServiceDefinitionRegistrationEndpointIntegrationTests {

    private static final String ENDPOINT = "/api/v1/catalog/services";

    private final MockMvc mockMvc;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    ServiceDefinitionRegistrationEndpointIntegrationTests(MockMvc mockMvc, JdbcTemplate jdbcTemplate) {
        this.mockMvc = mockMvc;
        this.jdbcTemplate = jdbcTemplate;
    }

    @BeforeEach
    void deleteServiceCatalog() {
        this.jdbcTemplate.update("DELETE FROM gw_service_operation_parameter");
        this.jdbcTemplate.update("DELETE FROM gw_service_operation");
        this.jdbcTemplate.update("DELETE FROM gw_service");
    }

    /**
     * 验证首次注册和相同请求重试能够穿过完整链路，并且数据库只保留一份聚合。
     *
     * @throws Exception HTTP 请求执行失败时抛出
     */
    @Test
    void shouldRegisterAndRetryCompleteDefinitionIdempotently() throws Exception {
        // When & Then
        this.mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("activity-application")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.serviceId").value("activity-service"))
                .andExpect(jsonPath("$.data.outcome").value("CREATED"));

        this.mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("activity-application")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.serviceId").value("activity-service"))
                .andExpect(jsonPath("$.data.outcome").value("UNCHANGED"));

        assertThat(count("gw_service")).isEqualTo(1);
        assertThat(count("gw_service_operation")).isEqualTo(1);
        assertThat(count("gw_service_operation_parameter")).isEqualTo(2);
        assertThat(applicationName()).isEqualTo("activity-application");
    }

    /**
     * 验证相同服务标识提交不同定义时返回稳定业务错误，并保留原始数据库内容。
     *
     * @throws Exception HTTP 请求执行失败时抛出
     */
    @Test
    void shouldRejectConflictingDefinitionThroughCompleteApplicationChain() throws Exception {
        // Given
        this.mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("activity-application")))
                .andExpect(status().isCreated());

        // When & Then
        this.mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "request-definition-conflict")
                        .content(requestBody("other-application")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(header().string(RequestIdFilter.REQUEST_ID_HEADER, "request-definition-conflict"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("CATALOG-0002"))
                .andExpect(jsonPath("$.message")
                        .value("Service activity-service already exists with a different definition"))
                .andExpect(jsonPath("$.data.path").value(ENDPOINT))
                .andExpect(jsonPath("$.data.requestId").value("request-definition-conflict"));

        assertThat(count("gw_service")).isEqualTo(1);
        assertThat(applicationName()).isEqualTo("activity-application");
    }

    private int count(String tableName) {
        return this.jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Integer.class);
    }

    private String applicationName() {
        return this.jdbcTemplate.queryForObject(
                "SELECT application_name FROM gw_service WHERE service_id = 'activity-service'", String.class);
    }

    private static String requestBody(String applicationName) {
        return """
                {
                  "serviceId": "activity-service",
                  "applicationName": "%s",
                  "protocol": "dubbo",
                  "namespace": "production",
                  "version": "1.0.0",
                  "operations": [
                    {
                      "operationId": "activity-query",
                      "interfaceName": "cn.example.ActivityService",
                      "operationName": "query",
                      "parameters": [
                        {
                          "position": 0,
                          "name": "name",
                          "typeName": "java.lang.String"
                        },
                        {
                          "position": 1,
                          "name": "age",
                          "typeName": "java.lang.Integer"
                        }
                      ]
                    }
                  ]
                }
                """.formatted(applicationName);
    }
}
