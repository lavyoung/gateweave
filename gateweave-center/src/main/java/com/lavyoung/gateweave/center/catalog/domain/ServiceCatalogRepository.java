package com.lavyoung.gateweave.center.catalog.domain;

import com.lavyoung.gateweave.center.shared.exception.BusinessException;
import com.lavyoung.gateweave.contract.catalog.CatalogRegistrationOutcome;

/**
 * 定义服务目录聚合的持久化注册能力。
 *
 * <p>实现必须在同一事务中注册服务定义、服务操作和操作参数，禁止留下只有服务、
 * 没有操作或只有操作、没有参数的部分状态。</p>
 *
 * <p>相同服务定义重复注册时返回 {@link CatalogRegistrationOutcome#UNCHANGED}。
 * 相同稳定标识对应不同定义时抛出业务异常，禁止静默覆盖已经注册的元数据。</p>
 *
 * <p>该端口不暴露 JDBC、Spring DAO 异常或数据库实体。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/01
 */
public interface ServiceCatalogRepository {

    /**
     * 原子、幂等地注册完整服务定义。
     *
     * @param serviceDefinition 包含操作和参数的完整服务定义
     * @return 首次创建时返回 CREATED，已有完全一致定义时返回 UNCHANGED
     * @throws NullPointerException serviceDefinition 为 null 时抛出
     * @throws BusinessException 稳定标识冲突、定义不一致或持久化失败时抛出
     */
    CatalogRegistrationOutcome register(ServiceDefinition serviceDefinition);
}
