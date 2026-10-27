package com.lavyoung.gateweave.engine.application.invocation;


import com.lavyoung.gateweave.contract.extension.InvocationRequest;
import com.lavyoung.gateweave.contract.extension.ProtocolInvoker;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * 根据调用请求中的协议标识选择对应的协议调用器。
 *
 * <p>分派器在创建时建立不可变的协议调用器索引，并拒绝重复协议。
 * 执行调用时只负责选择协议实现，不包含 Dubbo、HTTP 等具体协议逻辑。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/27
 */
public final class ProtocolInvocationDispatcher {

    private final Map<String, ProtocolInvoker> invokers;

    /**
     * 创建协议调用分派器。
     *
     * @param invokers 当前可用的协议调用器
     * @throws NullPointerException     调用器集合、集合元素或协议标识为 null 时抛出
     * @throws IllegalArgumentException 调用器集合为空、协议标识为空白或协议重复时抛出
     */
    public ProtocolInvocationDispatcher(List<ProtocolInvoker> invokers) {
        Objects.requireNonNull(invokers, "invokers must not be null");
        if (invokers.isEmpty()) {
            throw new IllegalArgumentException("invokers must not be empty");
        }
        Map<String, ProtocolInvoker> registryInvokers = new HashMap<>(invokers.size());
        for (ProtocolInvoker invoker : invokers) {
            Objects.requireNonNull(invoker, "invokers must not contain null");
            String protocol = Objects.requireNonNull(invoker.protocol(), "protocol invoker must return a protocol");
            if (protocol.isBlank()) {
                throw new IllegalArgumentException("protocol invoker protocol must not be blank");
            }
            String protocolStr = protocol.toLowerCase(Locale.ROOT);
            ProtocolInvoker previous = registryInvokers.putIfAbsent(protocolStr, invoker);
            if (previous != null) {
                throw new IllegalArgumentException("duplicate protocol invoker: " + protocolStr);
            }
        }
        this.invokers = Map.copyOf(registryInvokers);
    }

    /**
     * 将调用请求分派给对应的协议调用器。
     *
     * <p>协议未注册或调用器同步抛出异常时，返回失败的异步阶段，
     * 从而让应用服务使用统一的异步失败处理方式。</p>
     *
     * @param request 已完成路由映射的调用请求
     * @return 下游服务返回结果的异步阶段
     * @throws NullPointerException request 为 null 时抛出
     */
    public CompletionStage<String> invoke(InvocationRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        ProtocolInvoker invoker = invokers.get(request.protocol());
        if (invoker == null) {
            IllegalStateException failure = new IllegalStateException("No protocol invoker registered for: "
                    + request.protocol());
            return CompletableFuture.failedFuture(failure);
        }
        try {
            CompletionStage<String> completionStage = invoker.invoke(request);
            return Objects.requireNonNull(completionStage, "protocol invoker must not return null");
        } catch (RuntimeException exception) {
            return CompletableFuture.failedFuture(exception);
        }
    }
}
