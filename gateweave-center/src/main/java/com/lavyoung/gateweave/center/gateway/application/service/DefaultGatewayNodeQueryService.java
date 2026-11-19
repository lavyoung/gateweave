package com.lavyoung.gateweave.center.gateway.application.service;

import com.lavyoung.gateweave.center.gateway.application.GatewayNodePageQuery;
import com.lavyoung.gateweave.center.gateway.application.GatewayNodeQueryRepository;
import com.lavyoung.gateweave.center.gateway.application.QueryGatewayNodePageUseCase;
import com.lavyoung.gateweave.center.gateway.domain.GatewayNode;
import com.lavyoung.gateweave.center.shared.pagination.PageRequest;
import com.lavyoung.gateweave.center.shared.pagination.PageResult;

import java.util.List;
import java.util.Objects;

/**
 * 编排控制面网关节点分页查询。
 *
 * <p>应用服务先查询符合条件的节点总数。没有匹配节点时直接返回空页，
 * 避免继续执行没有意义的分页查询；存在数据时再加载当前页节点。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/18
 */
public class DefaultGatewayNodeQueryService implements QueryGatewayNodePageUseCase {

    private final GatewayNodeQueryRepository queryRepository;

    /**
     * 创建网关节点查询应用服务。
     *
     * @param queryRepository 网关节点查询持久化端口
     * @throws NullPointerException queryRepository 为 null 时抛出
     */
    public DefaultGatewayNodeQueryService(GatewayNodeQueryRepository queryRepository) {
        this.queryRepository = Objects.requireNonNull(queryRepository, "queryRepository must not be null");
    }

    @Override
    public PageResult<GatewayNode> query(GatewayNodePageQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        PageRequest pageRequest = query.pageRequest();
        long totalElements = this.queryRepository.countMatching(query);
        if (totalElements == 0) {
            return new PageResult<>(List.of(), pageRequest, 0);
        }
        List<GatewayNode> nodes = this.queryRepository.findMatchingPage(query);
        return new PageResult<>(nodes, pageRequest, totalElements);
    }
}
