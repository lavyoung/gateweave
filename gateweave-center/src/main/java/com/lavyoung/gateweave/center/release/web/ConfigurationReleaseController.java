package com.lavyoung.gateweave.center.release.web;

import com.lavyoung.gateweave.center.release.application.PublishGatewayConfigurationUseCase;
import com.lavyoung.gateweave.center.release.application.QueryPublishedGatewayConfigurationUseCase;
import com.lavyoung.gateweave.center.web.LocalizedResultFactory;
import com.lavyoung.gateweave.contract.configuration.GatewayConfigurationSnapshot;
import com.lavyoung.gateweave.contract.web.Result;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * 提供网关配置快照发布和读取接口。
 *
 * <p>管理端通过发布接口将当前候选配置固化为不可变快照。
 * Engine 只读取最新发布快照，不直接访问路由候选配置或控制面编辑数据。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/02
 */
@RestController
@RequestMapping("/api/v1/gateway-groups/{gatewayGroupId}/configuration-releases")
public class ConfigurationReleaseController {

    private final PublishGatewayConfigurationUseCase publishUseCase;
    private final QueryPublishedGatewayConfigurationUseCase queryUseCase;
    private final LocalizedResultFactory resultFactory;

    /**
     * 创建配置发布 Controller。
     *
     * @param publishUseCase 配置发布用例
     * @param queryUseCase   已发布配置查询用例
     * @param resultFactory  本地化统一响应工厂
     * @throws NullPointerException 任一依赖为 null 时抛出
     */
    public ConfigurationReleaseController(
            PublishGatewayConfigurationUseCase publishUseCase,
            QueryPublishedGatewayConfigurationUseCase queryUseCase, LocalizedResultFactory resultFactory) {
        this.publishUseCase = Objects.requireNonNull(publishUseCase, "publishUseCase must not be null");
        this.queryUseCase = Objects.requireNonNull(queryUseCase, "queryUseCase must not be null");
        this.resultFactory = Objects.requireNonNull(resultFactory, "resultFactory must not be null");
    }

    /**
     * 将指定网关分组的当前候选配置发布为新版本。
     *
     * @param gatewayGroupId 网关分组标识
     * @return HTTP 201 和本次发布的不可变配置快照
     */
    @PostMapping
    public ResponseEntity<Result<GatewayConfigurationSnapshot>> publish(
            @PathVariable("gatewayGroupId") @NotBlank String gatewayGroupId) {
        GatewayConfigurationSnapshot snapshot = this.publishUseCase.publish(gatewayGroupId);
        return ResponseEntity.status(HttpStatus.CREATED).body(this.resultFactory.success(snapshot));
    }

    /**
     * 查询指定网关分组最后一个已发布配置快照。
     *
     * @param gatewayGroupId 网关分组标识
     * @return 存在快照时返回 HTTP 200 和快照，没有发布记录时返回 HTTP 204
     */
    @GetMapping("/latest")
    public ResponseEntity<Result<GatewayConfigurationSnapshot>> queryLatest(
            @PathVariable("gatewayGroupId") @NotBlank String gatewayGroupId) {
        return this.queryUseCase.findLatestPublished(gatewayGroupId)
                .map(snapshot -> ResponseEntity.ok(this.resultFactory.success(snapshot)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
