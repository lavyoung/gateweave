package com.lavyoung.gateweave.starter.registration;

import com.lavyoung.gateweave.contract.catalog.CatalogRegistrationOutcome;
import com.lavyoung.gateweave.starter.metadata.ServiceMetadata;

/**
 * 定义 Starter 向 GateWeave 控制面注册服务的出站边界。
 *
 * <p>调用方只提交 Starter 已经采集完成的服务元数据，并接收稳定的幂等注册结果。
 * HTTP 请求构建、JSON 编解码和响应状态处理由具体适配器负责。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/13
 */
@FunctionalInterface
public interface ServiceRegistrationClient {

    /**
     * 注册一份完整服务定义。
     *
     * @param metadata 服务元数据
     * @return 首次创建或已有相同定义的注册结果
     * @throws NullPointerException  metadata 为 null 时抛出
     * @throws IllegalStateException 控制面请求失败、响应无效或服务定义冲突时抛出
     */
    CatalogRegistrationOutcome register(ServiceMetadata metadata);
}
