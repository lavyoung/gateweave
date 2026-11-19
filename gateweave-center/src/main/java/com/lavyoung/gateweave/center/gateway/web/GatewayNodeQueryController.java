package com.lavyoung.gateweave.center.gateway.web;

import com.lavyoung.gateweave.center.gateway.application.GatewayNodePageQuery;
import com.lavyoung.gateweave.center.gateway.application.QueryGatewayNodePageUseCase;
import com.lavyoung.gateweave.center.gateway.domain.GatewayNode;
import com.lavyoung.gateweave.center.gateway.domain.GatewayNodeStatus;
import com.lavyoung.gateweave.center.gateway.web.response.GatewayNodePageResponse;
import com.lavyoung.gateweave.center.shared.pagination.PageRequest;
import com.lavyoung.gateweave.center.shared.pagination.PageResult;
import com.lavyoung.gateweave.center.web.LocalizedResultFactory;
import com.lavyoung.gateweave.contract.web.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.Set;

/**
 * 提供控制面网关节点分页查询管理 API。
 *
 * <p>Controller 只解析和校验 HTTP 参数、调用应用用例并转换响应，
 * 不直接访问 JDBC 查询适配器。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/19
 */
@RestController
@RequestMapping("/gateway-nodes")
public class GatewayNodeQueryController {

    private final QueryGatewayNodePageUseCase queryGatewayNodePageUseCase;
    private final LocalizedResultFactory resultFactory;

    /**
     * 创建网关节点查询 Controller。
     *
     * @param queryGatewayNodePageUseCase 网关节点分页查询用例
     * @param resultFactory               本地化统一响应工厂
     * @throws NullPointerException 任一依赖为 null 时抛出
     */
    public GatewayNodeQueryController(QueryGatewayNodePageUseCase queryGatewayNodePageUseCase,
            LocalizedResultFactory resultFactory) {
        this.queryGatewayNodePageUseCase =
                Objects.requireNonNull(queryGatewayNodePageUseCase, "queryGatewayNodePageUseCase must not be null");
        this.resultFactory = Objects.requireNonNull(resultFactory, "resultFactory must not be null");
    }

    /**
     * 分页查询网关节点。
     *
     * <p>status 参数可以重复，例如
     * {@code status=ONLINE&status=OFFLINE}。不传入状态时查询全部状态。</p>
     *
     * @param nodeId         节点标识，空字符串表示不限制
     * @param gatewayGroupId 网关分组标识，空字符串表示不限制
     * @param statuses       节点状态集合，不传表示查询全部状态
     * @param pageNumber     页码，从 1 开始
     * @param pageSize       每页节点数量，最大为 100
     * @return 统一包装的网关节点分页响应
     */
    @GetMapping
    public Result<GatewayNodePageResponse> query(
            @RequestParam(name = "nodeId", defaultValue = "") String nodeId,
            @RequestParam(name = "gatewayGroupId", defaultValue = "") String gatewayGroupId,
            @RequestParam(name = "status", required = false) Set<GatewayNodeStatus> statuses,
            @RequestParam(name = "pageNumber", defaultValue = "1") int pageNumber,
            @RequestParam(name = "pageSize", defaultValue = "20") int pageSize) {
        PageRequest pageRequest = new PageRequest(pageNumber, pageSize);
        GatewayNodePageQuery query = new GatewayNodePageQuery(nodeId, gatewayGroupId, statuses, pageRequest);
        PageResult<GatewayNode> page = this.queryGatewayNodePageUseCase.query(query);
        return this.resultFactory.success(GatewayNodePageResponse.from(page));
    }
}
