package com.lavyoung.gateweave.starter.registration.http;

import com.lavyoung.gateweave.contract.catalog.RegisterServiceDefinitionRequest;
import com.lavyoung.gateweave.contract.catalog.ServiceDefinitionRegistrationResponse;
import com.lavyoung.gateweave.contract.web.Result;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.PostExchange;

/**
 * 声明 Starter 调用 GateWeave 控制面服务注册接口所需的 HTTP 契约。
 *
 * <p>Spring 根据该接口生成客户端代理，统一处理请求路径、JSON 编解码和响应类型。
 * 该接口只存在于 HTTP 适配器边界，不暴露给元数据采集和启动编排代码。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/13
 */
public interface ServiceRegistrationHttpApi {

    /**
     * 原子、幂等地注册完整服务定义。
     *
     * @param request 服务定义注册请求
     * @return 控制面统一响应及 HTTP 状态
     */
    @PostExchange(value = "/api/v1/catalog/services", contentType = MediaType.APPLICATION_JSON_VALUE,
            accept = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Result<ServiceDefinitionRegistrationResponse>> register(
            @RequestBody RegisterServiceDefinitionRequest request);
}
