package com.lavyoung.gateweave.engine.adapter.out.controlplane;

import com.fasterxml.jackson.databind.JsonNode;
import com.lavyoung.gateweave.contract.configuration.GatewayConfigurationSnapshot;
import com.lavyoung.gateweave.contract.gateway.RegisterGatewayNodeRequest;
import com.lavyoung.gateweave.contract.web.Result;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.PostExchange;

/**
 * 声明 Engine 访问控制面 HTTP API 所需的技术契约。
 *
 * <p>Spring HTTP Interface 根据该接口生成客户端代理，统一处理路径变量、请求体、
 * HTTP 状态和 JSON 消息转换。该接口只存在于出站适配器，不向应用层暴露。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/02
 */
public interface ControlPlaneHttpApi {

    /**
     * 注册或刷新当前网关节点。
     *
     * @param request 节点注册请求
     * @return 控制面统一响应及 HTTP 状态
     */
    @PostExchange(value = "/api/v1/gateway-nodes", contentType = MediaType.APPLICATION_JSON_VALUE,
            accept = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Result<JsonNode>> registerNode(@RequestBody RegisterGatewayNodeRequest request);

    /**
     * 查询网关分组最后一个已发布配置快照。
     *
     * @param gatewayGroupId 网关分组标识
     * @return 存在快照时返回 HTTP 200 和统一响应，不存在时返回 HTTP 204
     */
    @GetExchange(value = "/api/v1/gateway-groups/{gatewayGroupId}/configuration-releases/latest",
            accept = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Result<GatewayConfigurationSnapshot>> findLatestPublishedConfiguration(
            @PathVariable("gatewayGroupId") String gatewayGroupId);
}
