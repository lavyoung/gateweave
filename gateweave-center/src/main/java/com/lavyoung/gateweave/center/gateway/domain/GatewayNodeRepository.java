package com.lavyoung.gateweave.center.gateway.domain;

import java.time.Instant;

/**
 * 定义网关节点持久化能力。
 *
 * <p>实现必须保证注册、租约续期与租约过期处理具备并发安全性。相同 nodeId 的并发注册只能产生一条记录；
 * 心跳续期必须使用节点标识、租约标识和在线状态作为原子更新条件；
 * 租约过期处理必须根据在线状态和最后心跳时间批量更新。</p>
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

    /**
     * 使用当前运行租约原子记录节点心跳。
     *
     * <p>实现必须同时匹配 nodeId、leaseId、ONLINE 状态，并禁止配置版本倒退。
     * 不得先查询节点再执行无条件更新，否则旧实例可能覆盖重新注册后的新租约。</p>
     *
     * @param heartbeat 节点心跳领域命令
     * @return 心跳被当前在线租约接受时返回 true；节点不存在、租约失效、节点离线或版本倒退时返回 false
     * @throws NullPointerException heartbeat 为 null 时抛出
     */
    boolean renewLease(GatewayNodeHeartbeat heartbeat);

    /**
     * 将最后心跳时间不晚于截止时间的在线节点批量标记为离线。
     *
     * <p>实现必须通过一条条件更新完成状态判断和状态修改，禁止先查询再逐条更新。
     * 这样可以与并发到达的心跳续期通过数据库条件竞争，避免误下线刚刚完成续租的节点。</p>
     *
     * @param heartbeatDeadline 允许保持在线状态的最后心跳截止时间
     * @return 本次被标记为离线的节点数量
     * @throws NullPointerException heartbeatDeadline 为 null 时抛出
     */
    int markExpiredLeasesOffline(Instant heartbeatDeadline);
}
