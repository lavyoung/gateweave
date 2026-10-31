package com.lavyoung.gateweave.center.gateway.domain.repository;

import com.lavyoung.gateweave.center.gateway.domain.GatewayNode;
import com.lavyoung.gateweave.center.gateway.domain.GatewayNodeRegistrationResult;

/**
 * 定义网关节点持久化能力。
 *
 * <p>实现必须保证注册操作具备并发幂等性。相同 nodeId 的并发注册只能产生一条记录，
 * 已有节点重新注册时应刷新名称、地址、在线状态和最后心跳时间。</p>
 *
 * <p>该端口不暴露 JDBC、Spring DAO 异常或数据库实体。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/31
 */
public interface GatewayNodeRepository {

    /**
     * 创建新节点，或者刷新已经存在的节点。
     *
     * @param node 待注册的在线节点
     * @return 包含节点及 REGISTERED 或 REFRESHED 结果的领域结果
     * @throws NullPointerException node 为 null 时抛出
     */
    GatewayNodeRegistrationResult registerOrRefresh(GatewayNode node);
}
