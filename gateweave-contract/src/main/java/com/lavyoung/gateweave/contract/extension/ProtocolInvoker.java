package com.lavyoung.gateweave.contract.extension;

import java.util.concurrent.CompletionStage;

/**
 * 定义网关调用下游服务时使用的协议扩展契约。
 *
 * <p>每个实现负责一种协议，并通过唯一协议标识参与网关调用分派。
 * 调用请求只包含网关自身定义的契约，不允许暴露 Dubbo、HTTP 客户端等厂商 SDK 类型。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/27
 */
public interface ProtocolInvoker {

    /**
     * 返回当前调用器支持的协议标识。
     *
     * <p>协议标识必须唯一，并与 {@link InvocationRequest#protocol()} 的规范化结果一致。</p>
     *
     * @return 当前调用器支持的协议标识
     */
    String protocol();

    /**
     * 使用当前协议异步调用下游服务。
     *
     * @param request 已完成路由映射的调用请求
     * @return 下游服务返回结果的异步阶段
     * @throws NullPointerException request 为 null 时抛出
     * @throws RuntimeException     调用提交失败时抛出
     */
    CompletionStage<String> invoke(InvocationRequest request);

}
