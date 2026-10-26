package com.lavyoung.gateweave.plugin.invoker.rpc;

import java.lang.reflect.Proxy;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.dubbo.rpc.service.GenericService;

import com.lavyoung.gateweave.contract.extension.IGenericReference;

/**
 * 创建并缓存绑定到 Dubbo RPC 方法的泛化调用代理。
 *
 * <p>每个工厂对应一个 Dubbo GenericService。同一个 RPC 方法只创建一个代理对象，
 * 后续调用复用缓存中的线程安全代理。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @since 1.0
 */
public final class GenericReferenceProxyFactory {

    private final GenericService genericService;
    private final ConcurrentHashMap<String, IGenericReference> genericReferenceCache = new ConcurrentHashMap<>();

    /**
     * 创建指定 Dubbo 泛化服务的代理工厂。
     *
     * @param genericService Dubbo 泛化服务
     * @throws NullPointerException genericService 为 null 时抛出
     */
    public GenericReferenceProxyFactory(GenericService genericService) {
        this.genericService = Objects.requireNonNull(genericService, "genericService must not be null");
    }

    /**
     * 获取绑定到指定 RPC 方法的泛化调用代理。
     *
     * @param methodName 目标 RPC 方法名称
     * @return 可执行泛化调用的代理对象
     * @throws NullPointerException     methodName 为 null 时抛出
     * @throws IllegalArgumentException methodName 为空白时抛出
     */
    public IGenericReference newInstance(String methodName) {
        Objects.requireNonNull(methodName, "methodName must not be null");
        return this.genericReferenceCache.computeIfAbsent(methodName, name -> {
            GenericReferenceProxy proxy = new GenericReferenceProxy(this.genericService, methodName);
            ClassLoader classLoader = IGenericReference.class.getClassLoader();
            Class<?>[] interfaces = {IGenericReference.class};
            return (IGenericReference) Proxy.newProxyInstance(classLoader, interfaces, proxy);
        });
    }
}
