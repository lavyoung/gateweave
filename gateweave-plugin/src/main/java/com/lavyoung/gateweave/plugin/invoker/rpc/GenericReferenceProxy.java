package com.lavyoung.gateweave.plugin.invoker.rpc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.apache.dubbo.rpc.service.GenericService;

import com.lavyoung.gateweave.contract.extension.IGenericReference;

/**
 * 将统一泛化调用接口转换为 Dubbo GenericService 调用。
 *
 * <p>该代理隐藏 Dubbo SDK 类型，根据代理方法提取方法参数类型，
 * 最终调用绑定的 Dubbo RPC 方法。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @since 1.0
 */
public final class GenericReferenceProxy implements InvocationHandler {

    private final GenericService genericService;
    private final String methodName;

    /**
     * 创建绑定到指定 Dubbo 方法的泛化调用代理。
     *
     * @param genericService Dubbo 泛化服务
     * @param methodName     目标 RPC 方法名称
     * @throws NullPointerException     genericService 或 methodName 为 null 时抛出
     * @throws IllegalArgumentException methodName 为空白时抛出
     */
    public GenericReferenceProxy(GenericService genericService, String methodName) {
        this.genericService = Objects.requireNonNull(genericService, "genericService must not be null");
        Objects.requireNonNull(methodName, "methodName must not be null");
        if (methodName.isBlank()) {
            throw new IllegalArgumentException("methodName must not be blank");
        }
        this.methodName = methodName;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Objects.requireNonNull(method, "method must not be null");
        if (method.getDeclaringClass() != IGenericReference.class) {
            throw new UnsupportedOperationException("unsupported proxy method: %s".formatted(method.getName()));
        }
        Objects.requireNonNull(args, "args must not be null");
        String[] parameterTypes = Arrays.stream(method.getParameterTypes()).map(Class::getName).toArray(String[]::new);
        CompletableFuture<Object> resultFuture =
                this.genericService.$invokeAsync(this.methodName, parameterTypes, args);
        return resultFuture.thenApply(result -> {
            if (!(result instanceof String response)) {
                throw new IllegalStateException("generic invocation result must be a String");
            }
            return response;
        });
    }
}
