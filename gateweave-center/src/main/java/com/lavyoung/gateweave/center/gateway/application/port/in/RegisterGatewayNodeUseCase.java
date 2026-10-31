package com.lavyoung.gateweave.center.gateway.application.port.in;

import com.lavyoung.gateweave.center.gateway.domain.GatewayNodeRegistration;
import com.lavyoung.gateweave.center.gateway.domain.GatewayNodeRegistrationResult;

/**
 * 定义网关算力节点注册的入站用例。
 *
 * <p>HTTP Controller 及其他入站适配器必须通过该端口注册节点，
 * 不得直接访问节点 Repository。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/31
 */
@FunctionalInterface
public interface RegisterGatewayNodeUseCase {

    /**
     * 注册新节点，或者刷新已经存在的节点。
     *
     * @param registration 已完成领域校验的节点注册信息
     * @return 节点注册结果
     * @throws NullPointerException registration 为 null 时抛出
     */
    GatewayNodeRegistrationResult register(GatewayNodeRegistration registration);
}
