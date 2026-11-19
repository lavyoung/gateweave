package com.lavyoung.gateweave.center.configuration;

import com.lavyoung.gateweave.center.gateway.application.QueryGatewayNodePageUseCase;
import com.lavyoung.gateweave.center.gateway.web.GatewayNodeQueryController;
import com.lavyoung.gateweave.center.web.LocalizedResultFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 验证管理后台跨域预检请求只能从配置允许的来源访问控制面 API。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/19
 */
@WebMvcTest(GatewayNodeQueryController.class)
@MockBean({QueryGatewayNodePageUseCase.class, LocalizedResultFactory.class})
class ControlPlaneCorsTests {

    private final MockMvc mockMvc;

    @Autowired
    ControlPlaneCorsTests(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void shouldAllowConfiguredAdminOrigin() throws Exception {
        this.mockMvc.perform(options("/api/v1/gateway-nodes")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET,POST,PUT,DELETE"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "X-Request-Id"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "1800"));
    }

    @Test
    void shouldRejectUnconfiguredOrigin() throws Exception {
        this.mockMvc.perform(options("/api/v1/gateway-nodes")
                        .header(HttpHeaders.ORIGIN, "https://untrusted.example.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }
}
