package com.lavyoung.gateweave.engine.application.port.out;

import com.lavyoung.gateweave.contract.configuration.GatewayConfigurationSnapshot;
import com.lavyoung.gateweave.contract.gateway.RegisterGatewayNodeRequest;

import java.util.Optional;

/**
 * 定义 Engine 访问控制面的稳定出站端口。
 *
 * <p>该端口只暴露节点注册和已发布配置查询语义，不暴露 HTTP 状态码、
 * JSON、Spring、具体客户端或控制面的数据库模型。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/02
 */
public interface GatewayControlPlaneClient {

    /**
     * 向控制面注册或刷新当前网关节点。
     *
     * @param request 节点注册契约
     * @throws NullPointerException  request 为 null 时抛出
     * @throws IllegalStateException 控制面通信失败、响应无法解析或注册被拒绝时抛出
     */
    void registerNode(RegisterGatewayNodeRequest request);

    /**
     * 查询指定网关分组最后一个已发布配置快照。
     *
     * @param gatewayGroupId 网关分组标识
     * @return 存在已发布快照时返回快照，控制面返回 HTTP 204 时返回空
     * @throws NullPointerException     gatewayGroupId 为 null 时抛出
     * @throws IllegalArgumentException gatewayGroupId 为空白时抛出
     * @throws IllegalStateException    控制面通信失败、响应无法解析或查询被拒绝时抛出
     */
    Optional<GatewayConfigurationSnapshot> findLatestPublishedConfiguration(String gatewayGroupId);
}
