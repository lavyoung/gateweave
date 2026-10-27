package com.lavyoung.gateweave.plugin.invoker.rpc;

import com.lavyoung.gateweave.contract.extension.IGenericReference;
import com.lavyoung.gateweave.contract.extension.InvocationRequest;
import com.lavyoung.gateweave.contract.extension.ProtocolInvoker;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.CompletionStage;

/**
 * 使用 Dubbo 泛化引用执行统一调用请求。
 *
 * <p>该适配器负责解析 Dubbo 专属属性、获取已经注册的泛化引用，
 * 并将请求负载转换为当前 RPC 方法需要的字符串参数。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/27
 */
public final class DubboProtocolInvoker implements ProtocolInvoker {

    private static final String PROTOCOL = "dubbo";

    private final GenericReferenceRegistry referenceRegistry;

    /**
     * 创建 Dubbo 协议调用器。
     *
     * @param referenceRegistry Dubbo 泛化引用注册表
     * @throws NullPointerException referenceRegistry 为 null 时抛出
     */
    public DubboProtocolInvoker(GenericReferenceRegistry referenceRegistry) {
        this.referenceRegistry = Objects.requireNonNull(referenceRegistry, "referenceRegistry must not be null");
    }

    @Override
    public String protocol() {
        return PROTOCOL;
    }

    @Override
    public CompletionStage<String> invoke(InvocationRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        DubboReferenceOptions options = DubboReferenceOptions.from(request.attributes());
        IGenericReference reference = this.referenceRegistry.getGenericReference(
                request.service(), request.operation(), options.version());
        String argument = new String(request.payload(), StandardCharsets.UTF_8);
        CompletionStage<String> completionStage = reference.$invoke(argument);
        return Objects.requireNonNull(completionStage, "generic reference must not return null");
    }
}
