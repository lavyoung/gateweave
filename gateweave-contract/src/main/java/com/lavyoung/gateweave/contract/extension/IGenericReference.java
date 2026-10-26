package com.lavyoung.gateweave.contract.extension;

import java.util.concurrent.CompletionStage;

/**
 * 定义网关调用 RPC 服务时使用的统一泛化调用接口。
 *
 * <p>网关不依赖 RPC 服务提供方的接口包，而是通过该接口异步调用代理对象。
 * 代理对象负责把调用转换为 Dubbo GenericService 的泛化调用。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @since 1.0
 */
public interface IGenericReference {

    /**
     * 异步调用当前代理绑定的 RPC 方法。
     *
     * @param argument RPC 方法参数
     * @return RPC 服务返回结果的异步阶段
     * @throws RuntimeException RPC 调用提交失败时抛出
     */
    CompletionStage<String> $invoke(String argument);
}
