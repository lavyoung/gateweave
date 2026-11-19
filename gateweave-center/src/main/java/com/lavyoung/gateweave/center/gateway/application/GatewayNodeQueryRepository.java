package com.lavyoung.gateweave.center.gateway.application;

import com.lavyoung.gateweave.center.gateway.domain.GatewayNode;

import java.util.List;

/**
 * 定义网关节点管理查询所需的持久化能力。
 *
 * <p>该端口只描述应用层需要的数量统计和分页读取，不承担节点注册、
 * 续租、下线等领域写入职责。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/18
 */
public interface GatewayNodeQueryRepository {

    /**
     * 统计符合筛选条件的网关节点数量。
     *
     * <p>实现只使用查询中的节点标识、网关分组和状态集合，
     * 不使用页码及每页数量。</p>
     *
     * @param query 网关节点查询条件
     * @return 符合条件的节点数量
     * @throws NullPointerException query 为 null 时抛出
     */
    long countMatching(GatewayNodePageQuery query);

    /**
     * 查询指定分页范围内的网关节点。
     *
     * <p>实现必须在数据库中完成筛选、排序和分页，禁止读取全部节点后在内存中分页。</p>
     *
     * @param query 网关节点分页查询条件
     * @return 当前页网关节点
     * @throws NullPointerException query 为 null 时抛出
     */
    List<GatewayNode> findMatchingPage(GatewayNodePageQuery query);
}
