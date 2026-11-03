package com.lavyoung.gateweave.engine.application.service;

import com.lavyoung.gateweave.contract.configuration.GatewayConfigurationSnapshot;
import com.lavyoung.gateweave.engine.application.invocation.ProtocolInvocationDispatcher;
import com.lavyoung.gateweave.engine.application.mapper.GatewayRouteConfigurationMapper;
import com.lavyoung.gateweave.engine.application.port.out.GatewayControlPlaneClient;
import com.lavyoung.gateweave.engine.domain.route.GatewayRoute;
import com.lavyoung.gateweave.engine.domain.route.GatewayRouteTable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 将控制面最后发布的配置快照同步到数据面运行时路由表。
 *
 * <p>该服务先将完整契约快照转换为领域路由，全部转换成功后才尝试原子替换路由表。
 * 查询失败或配置转换失败时异常直接向调用方传播，当前生效路由不会被修改。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/02
 */
public final class GatewayConfigurationSynchronizer {

    private final GatewayControlPlaneClient controlPlaneClient;
    private final GatewayRouteConfigurationMapper routeMapper;
    private final ProtocolInvocationDispatcher invocationDispatcher;
    private final GatewayRouteTable routeTable;

    /**
     * 创建网关配置同步应用服务。
     *
     * @param controlPlaneClient   控制面出站端口
     * @param routeMapper          配置契约到领域路由的转换器
     * @param invocationDispatcher 协议调用与资源准备分派器
     * @param routeTable           当前运行时路由表
     * @throws NullPointerException 任一依赖为 null 时抛出
     */
    public GatewayConfigurationSynchronizer(GatewayControlPlaneClient controlPlaneClient,
            GatewayRouteConfigurationMapper routeMapper, ProtocolInvocationDispatcher invocationDispatcher,
            GatewayRouteTable routeTable) {
        this.controlPlaneClient = Objects.requireNonNull(
                controlPlaneClient, "controlPlaneClient must not be null");
        this.routeMapper = Objects.requireNonNull(routeMapper, "routeMapper must not be null");
        this.invocationDispatcher = Objects.requireNonNull(
                invocationDispatcher, "invocationDispatcher must not be null");
        this.routeTable = Objects.requireNonNull(routeTable, "routeTable must not be null");
    }
    /**
     * 查询并应用指定网关分组最后发布的配置快照。
     *
     * @param gatewayGroupId 网关分组标识
     * @return 应用了更高版本返回 true；没有已发布快照或版本未更新返回 false
     * @throws NullPointerException  gatewayGroupId 为 null 时抛出
     * @throws IllegalStateException 控制面查询失败或快照无法转换时抛出
     */
    public boolean synchronize(String gatewayGroupId) {
        Objects.requireNonNull(gatewayGroupId, "gatewayGroupId must not be null");
        Optional<GatewayConfigurationSnapshot> snapshotOptional =
                this.controlPlaneClient.findLatestPublishedConfiguration(gatewayGroupId);
        if (snapshotOptional.isEmpty()) {
            return false;
        }
        GatewayConfigurationSnapshot snapshot = snapshotOptional.orElseThrow();
        if (snapshot.version() <= this.routeTable.currentVersion()) {
            return false;
        }
        List<GatewayRoute> routes = snapshot.routes().stream().map(this.routeMapper::toDomain).toList();
        snapshot.routes().forEach(route -> this.invocationDispatcher.prepare(route.target()));
        return this.routeTable.replaceIfNew(snapshot.version(), routes);
    }
}
