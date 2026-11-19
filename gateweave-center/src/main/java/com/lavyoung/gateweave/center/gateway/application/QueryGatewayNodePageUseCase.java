package com.lavyoung.gateweave.center.gateway.application;

import com.lavyoung.gateweave.center.gateway.domain.GatewayNode;
import com.lavyoung.gateweave.center.shared.pagination.PageResult;

/**
 * 定义控制面分页查询网关节点的应用用例。
 *
 * <p>入站适配器只依赖该接口，不直接访问网关节点 Repository。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/17
 */
public interface QueryGatewayNodePageUseCase {

    /**
     * 根据筛选条件分页查询网关节点。
     *
     * @param query 节点分页查询条件
     * @return 网关节点分页结果
     * @throws NullPointerException query 为 null 时抛出
     */
    PageResult<GatewayNode> query(GatewayNodePageQuery query);
}
