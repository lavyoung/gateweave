package com.lavyoung.gateweave.plugin.invoker.rpc;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.dubbo.common.config.ReferenceCache;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.utils.SimpleReferenceCache;
import org.apache.dubbo.rpc.model.ScopeModel;
import org.apache.dubbo.rpc.service.GenericService;

import com.lavyoung.gateweave.contract.extension.GenericInvocationArguments;
import com.lavyoung.gateweave.contract.extension.IGenericReference;

import static org.apache.dubbo.registry.Constants.MIGRATION_STEP_KEY;

/**
 * 注册并管理网关当前可调用的 Dubbo 泛化服务。
 *
 * <p>注册阶段通过 Dubbo ReferenceCache 取得 GenericService，并为 RPC 方法建立统一调用函数。
 * 请求处理阶段可以根据方法名称取得统一的泛化调用接口。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @since 1.0
 */
public final class GenericReferenceRegistry implements AutoCloseable {

    private final AtomicBoolean closed = new AtomicBoolean();

    private final DubboDiscoveryMode discoveryMode;
    private final ApplicationConfig applicationConfig;
    private final RegistryConfig registryConfig;
    private final ReferenceCache referenceCache = SimpleReferenceCache.newCache();
    private final ConcurrentHashMap<GenericReferenceKey, IGenericReference> knownGenericReferences =
            new ConcurrentHashMap<>();

    /**
     * 创建 Dubbo 泛化服务注册器。
     *
     * @param applicationName Dubbo 消费端应用名称
     * @param registryAddress 注册中心地址，例如 zookeeper://127.0.0.1:2181
     * @param discoveryMode   Dubbo 服务发现模式
     * @throws NullPointerException     参数为 null 时抛出
     * @throws IllegalArgumentException 字符串参数为空白时抛出
     */
    public GenericReferenceRegistry(String applicationName, String registryAddress, DubboDiscoveryMode discoveryMode) {
        Objects.requireNonNull(applicationName, "applicationName must not be null");
        Objects.requireNonNull(registryAddress, "registryAddress must not be null");
        this.discoveryMode = Objects.requireNonNull(discoveryMode, "discoveryMode must not be null");

        if (applicationName.isBlank()) {
            throw new IllegalArgumentException("applicationName must not be blank");
        }
        if (registryAddress.isBlank()) {
            throw new IllegalArgumentException("registryAddress must not be blank");
        }

        this.applicationConfig = new ApplicationConfig(applicationName);
        // 关闭 22222。
        this.applicationConfig.setQosEnable(false);
        this.applicationConfig.setRegisterMode(this.discoveryMode.registerMode());
        this.registryConfig = new RegistryConfig(registryAddress);
    }

    /**
     * 注册一个 Dubbo 接口方法的泛化调用代理。
     *
     * <p>同一方法重复注册时复用首次创建的统一调用函数。</p>
     *
     * @param interfaceName Dubbo 服务接口全限定名
     * @param methodName    RPC 方法名称
     * @param version       Dubbo 服务版本
     * @throws NullPointerException     参数为 null 时抛出
     * @throws IllegalArgumentException 参数为空白时抛出
     * @throws RuntimeException         Dubbo 服务引用创建失败时抛出
     */
    public void addGenericReference(String interfaceName, String methodName, String version) {
        GenericReferenceKey key = new GenericReferenceKey(interfaceName, methodName, version);
        this.knownGenericReferences.computeIfAbsent(key, this::createGenericReference);
    }

    /**
     * 获取指定 Dubbo 接口方法的泛化调用代理。
     *
     * @param interfaceName Dubbo 服务接口全限定名
     * @param methodName    RPC 方法名称
     * @param version       Dubbo 服务版本
     * @return 已注册的泛化调用代理
     * @throws NullPointerException     任一参数为 null 时抛出
     * @throws IllegalArgumentException 任一参数为空白时抛出
     * @throws IllegalStateException    对应服务方法尚未注册时抛出
     */
    public IGenericReference getGenericReference(String interfaceName, String methodName, String version) {
        GenericReferenceKey key = new GenericReferenceKey(interfaceName, methodName, version);
        IGenericReference genericReference = this.knownGenericReferences.get(key);
        if (genericReference == null) {
            throw new IllegalStateException(
                    "generic reference is not registered: %s#%s:%s".formatted(interfaceName, methodName, version));
        }
        return genericReference;
    }

    /**
     * 释放所有 Dubbo 服务引用及其连接资源。
     *
     * <p>Dubbo ShutdownHook 可能先于 Spring 销毁回调执行。模型已销毁时不再重复释放引用；
     * 主动关闭失败时恢复关闭状态，以便调用方重试。</p>
     *
     * @throws RuntimeException Dubbo 模型仍存活且引用释放失败时抛出
     */
    @Override
    public void close() {
        if (!this.closed.compareAndSet(false, true)) {
            return;
        }
        this.knownGenericReferences.clear();
        ScopeModel scopeModel = this.applicationConfig.getScopeModel();
        if (scopeModel.isDestroyed()) {
            return;
        }
        try {
            this.referenceCache.destroyAll();
        } catch (RuntimeException e) {
            if (!scopeModel.isDestroyed()) {
                this.closed.set(false);
                throw e;
            }
        }
    }

    /**
     * 创建目标 Dubbo 服务引用及统一调用函数。
     *
     * @param key Dubbo 服务引用唯一键
     * @return 绑定到指定 Dubbo 方法的统一调用函数
     * @throws RuntimeException Dubbo 服务引用创建失败时抛出
     */
    private IGenericReference createGenericReference(GenericReferenceKey key) {
        ReferenceConfig<GenericService> referenceConfig = new ReferenceConfig<>();
        // todo 方法过时处理
        referenceConfig.setApplication(this.applicationConfig);
        referenceConfig.setRegistry(this.registryConfig);
        referenceConfig.setInterface(key.interfaceName());
        referenceConfig.setVersion(key.version());
        referenceConfig.setGeneric("true");
        referenceConfig.setCheck(false);
        referenceConfig.setParameters(Map.of(MIGRATION_STEP_KEY, this.discoveryMode.migrationStep()));

        GenericService genericService = this.referenceCache.get(referenceConfig);
        Objects.requireNonNull(genericService, "Dubbo GenericService must not be null");
        return invocationArguments -> invoke(genericService, key.methodName(), invocationArguments);
    }

    /**
     * 将项目统一调用参数转换为 Dubbo 泛化调用参数并校验返回类型。
     *
     * @param genericService     Dubbo 官方泛化服务
     * @param methodName        目标方法名称
     * @param invocationArguments 项目统一调用参数
     * @return 异步字符串调用结果
     * @throws NullPointerException invocationArguments 为 null 时抛出
     */
    private CompletionStage<String> invoke(GenericService genericService, String methodName,
            GenericInvocationArguments invocationArguments) {
        Objects.requireNonNull(invocationArguments, "invocationArguments must not be null");
        String[] parameterTypes = invocationArguments.parameterTypes().toArray(String[]::new);
        Object[] arguments = invocationArguments.arguments().toArray();
        return genericService.$invokeAsync(methodName, parameterTypes, arguments).thenApply(result -> {
            if (!(result instanceof String response)) {
                throw new IllegalStateException("generic invocation result must be a String");
            }
            return response;
        });
    }

    /**
     * 唯一标识一个可调用的 Dubbo 接口方法。
     *
     * @param interfaceName Dubbo 服务接口全限定名
     * @param methodName    RPC 方法名称
     * @param version       Dubbo 服务版本
     */
    private record GenericReferenceKey(String interfaceName, String methodName, String version) {

        /**
         * 校验 Dubbo 服务引用唯一键。
         *
         * @throws NullPointerException     任一属性为 null 时抛出
         * @throws IllegalArgumentException 任一属性为空白时抛出
         */
        private GenericReferenceKey {
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
        }
    }
}
