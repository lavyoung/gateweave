package com.lavyoung.gateweave.contract.extension;

import com.lavyoung.gateweave.contract.configuration.InvocationTargetConfiguration;

import java.util.Objects;
import java.util.concurrent.CompletionStage;

/**
 * 定义网关调用下游服务时使用的协议扩展契约。
 *
 * <p>每个实现负责一种协议，并通过唯一协议标识参与网关调用分派。
 * 调用请求和目标配置只包含网关自身定义的契约，不允许暴露厂商 SDK 类型。</p>
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
     * 为即将生效的调用目标准备协议运行时资源。
     *
     * <p>该方法必须具有幂等性。HTTP 等无需提前建立运行时资源的协议可以使用默认实现；
     * Dubbo 等需要服务引用的协议应在此完成引用注册。方法返回后，该目标必须能够被调用。</p>
     *
     * @param target 即将生效的协议调用目标配置
     * @throws NullPointerException target 为 null 时抛出
     * @throws RuntimeException     运行时资源准备失败时抛出
     */
    default void prepare(InvocationTargetConfiguration target) {
        // 默认协议不需要提前准备运行时资源。
        Objects.requireNonNull(target, "target must not be null");
    }

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
