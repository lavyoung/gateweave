package com.lavyoung.gateweave.plugin.invoker.rpc;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.dubbo.common.config.ReferenceCache;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.utils.SimpleReferenceCache;
import org.apache.dubbo.rpc.service.GenericService;

import com.lavyoung.gateweave.contract.extension.IGenericReference;

/**
 * 注册并管理网关当前可调用的 Dubbo 泛化服务。
 *
 * <p>注册阶段根据接口名称创建 Dubbo GenericService，并为 RPC 方法建立代理工厂。
 * 请求处理阶段可以根据方法名称取得统一的泛化调用接口。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @since 1.0
 */
public final class GenericReferenceRegistry implements AutoCloseable {

    private final ApplicationConfig applicationConfig;
    private final RegistryConfig registryConfig;
    private final ReferenceCache referenceCache = SimpleReferenceCache.newCache();
    private final ConcurrentHashMap<String, GenericReferenceProxyFactory> knownGenericReferences =
            new ConcurrentHashMap<>();

    /**
     * 创建 Dubbo 泛化服务注册器。
     *
     * @param applicationName Dubbo 消费端应用名称
     * @param registryAddress 注册中心地址，例如 zookeeper://127.0.0.1:2181
     * @throws NullPointerException     参数为 null 时抛出
     * @throws IllegalArgumentException 参数为空白时抛出
     */
    public GenericReferenceRegistry(String applicationName, String registryAddress) {
        Objects.requireNonNull(applicationName, "applicationName must not be null");
        Objects.requireNonNull(registryAddress, "registryAddress must not be null");
        if (applicationName.isBlank()) {
            throw new IllegalArgumentException("applicationName must not be blank");
        }
        if (registryAddress.isBlank()) {
            throw new IllegalArgumentException("registryAddress must not be blank");
        }
        this.applicationConfig = new ApplicationConfig(applicationName);
        this.registryConfig = new RegistryConfig(registryAddress);
    }

    /**
     * 注册一个 Dubbo 接口方法的泛化调用代理。
     *
     * <p>同一方法重复注册时复用首次创建的代理工厂。</p>
     *
     * @param interfaceName Dubbo 服务接口全限定名
     * @param methodName    RPC 方法名称
     * @param version       Dubbo 服务版本
     * @throws NullPointerException     参数为 null 时抛出
     * @throws IllegalArgumentException 参数为空白时抛出
     * @throws RuntimeException         Dubbo 服务引用创建失败时抛出
     */
    public void addGenericReference(String interfaceName, String methodName, String version) {
        Objects.requireNonNull(interfaceName, "interfaceName must not be null");
        Objects.requireNonNull(methodName, "methodName must not be null");
        Objects.requireNonNull(version, "version must not be null");
        if (interfaceName.isBlank()) {
            throw new IllegalArgumentException("interfaceName must not be blank");
        }
        if (methodName.isBlank()) {
            throw new IllegalArgumentException("methodName must not be blank");
        }
        if (version.isBlank()) {
            throw new IllegalArgumentException("version must not be blank");
        }
        this.knownGenericReferences.computeIfAbsent(methodName, ignored ->
                createProxyFactory(interfaceName, version));
    }

    /**
     * 获取指定 RPC 方法的泛化调用代理。
     *
     * @param methodName RPC 方法名称
     * @return 已注册的泛化调用代理
     * @throws NullPointerException  methodName 为 null 时抛出
     * @throws IllegalStateException 方法尚未注册时抛出
     */
    public IGenericReference getGenericReference(String methodName) {
        Objects.requireNonNull(methodName, "methodName must not be null");
        GenericReferenceProxyFactory proxyFactory = this.knownGenericReferences.get(methodName);
        if (proxyFactory == null) {
            throw new IllegalStateException("generic reference is not registered: %s".formatted(methodName));
        }
        return proxyFactory.newInstance(methodName);
    }

    /**
     * 释放所有 Dubbo 服务引用及其连接资源。
     */
    @Override
    public void close() {
        this.knownGenericReferences.clear();
        this.referenceCache.destroyAll();
    }

    /**
     * 创建目标 Dubbo 服务引用及其代理工厂。
     *
     * @param interfaceName Dubbo 服务接口全限定名
     * @param version       Dubbo 服务版本
     * @return 泛化调用代理工厂
     * @throws RuntimeException Dubbo 服务引用创建失败时抛出
     */
    private GenericReferenceProxyFactory createProxyFactory(String interfaceName, String version) {
        ReferenceConfig<GenericService> referenceConfig = new ReferenceConfig<>();
        referenceConfig.setApplication(this.applicationConfig);
        referenceConfig.setRegistry(this.registryConfig);
        referenceConfig.setInterface(interfaceName);
        referenceConfig.setVersion(version);
        referenceConfig.setGeneric("true");
        referenceConfig.setCheck(false);

        GenericService genericService = this.referenceCache.get(referenceConfig);
        Objects.requireNonNull(genericService, "Dubbo GenericService must not be null");
        return new GenericReferenceProxyFactory(genericService);
    }
}
