package com.lavyoung.gateweave.center.catalog.application.port.in;

import com.lavyoung.gateweave.center.catalog.domain.ServiceDefinition;
import com.lavyoung.gateweave.center.shared.exception.BusinessException;
import com.lavyoung.gateweave.contract.catalog.CatalogRegistrationOutcome;

/**
 * 定义完整服务元数据的幂等注册用例。
 *
 * <p>标准 HTTP 客户端和后续 Starter 共享该用例，避免形成两套服务注册模型。
 * 入站适配器负责将外部请求转换为 {@link ServiceDefinition}，应用层不依赖 HTTP DTO。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/01
 */
public interface RegisterServiceDefinitionUseCase {

    /**
     * 注册包含操作和参数的完整服务定义。
     *
     * @param serviceDefinition 完整服务定义
     * @return 首次创建时返回 CREATED，重复注册相同定义时返回 UNCHANGED
     * @throws NullPointerException serviceDefinition 为 null 时抛出
     * @throws BusinessException 服务定义冲突或持久化失败时抛出
     */
    CatalogRegistrationOutcome register(ServiceDefinition serviceDefinition);
}
