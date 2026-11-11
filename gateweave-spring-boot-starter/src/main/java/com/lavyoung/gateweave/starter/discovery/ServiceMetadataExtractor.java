package com.lavyoung.gateweave.starter.discovery;

import com.lavyoung.gateweave.starter.annotation.GateWeaveOperation;
import com.lavyoung.gateweave.starter.annotation.GateWeaveService;
import com.lavyoung.gateweave.starter.metadata.ServiceMetadata;
import com.lavyoung.gateweave.starter.metadata.ServiceOperationMetadata;
import com.lavyoung.gateweave.starter.metadata.ServiceParameterMetadata;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * 从 Java 服务类型及其接口中提取 GateWeave 服务元数据。
 *
 * <p>提取器只负责类型检查和模型转换。Spring Bean 发现、代理类型解析、
 * 元数据保存及控制面注册由外层组件处理。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/11
 */
public final class ServiceMetadataExtractor {

    private final ParameterNameDiscoverer parameterNameDiscoverer;

    /**
     * 创建服务元数据提取器。
     *
     * @param parameterNameDiscoverer Spring 参数名称发现器
     * @throws NullPointerException parameterNameDiscoverer 为 null 时抛出
     */
    public ServiceMetadataExtractor(ParameterNameDiscoverer parameterNameDiscoverer) {
        this.parameterNameDiscoverer =
                Objects.requireNonNull(parameterNameDiscoverer, "parameterNameDiscoverer must not be null");
    }

    /**
     * 从服务实现类型及其接口中提取全部 GateWeave 服务定义。
     *
     * <p>未标记的类型返回空列表。一个实现类同时实现多个已标记接口时，
     * 每个接口分别形成一份服务元数据。</p>
     *
     * @param beanType        服务 Bean 的真实用户类型
     * @param applicationName 服务所属应用名称
     * @return 按服务类型名称稳定排列的不可变元数据列表
     * @throws NullPointerException     beanType 或 applicationName 为 null 时抛出
     * @throws IllegalArgumentException applicationName 为空白，或服务定义违反元数据约束时抛出
     * @throws IllegalStateException    无法读取方法参数名称时抛出
     */
    public List<ServiceMetadata> extract(Class<?> beanType, String applicationName) {
        Objects.requireNonNull(beanType, "beanType must not be null");
        String checkedApplicationName =
                Objects.requireNonNull(applicationName, "applicationName must not be null").strip();
        if (checkedApplicationName.isEmpty()) {
            throw new IllegalArgumentException("applicationName must not be blank");
        }
        List<ServiceMetadata> services = findServiceTypes(beanType).stream()
                .map(serviceType -> extractService(serviceType, checkedApplicationName))
                .sorted(Comparator.comparing(ServiceMetadata::serviceId)).toList();
        long serviceIdCount = services.stream().map(ServiceMetadata::serviceId).distinct().count();
        if (serviceIdCount != services.size()) {
            throw new IllegalArgumentException("serviceIds must not be duplicated");
        }
        return services;
    }

    /**
     * 查找实现类型自身、父类及所有接口中直接标记的服务类型。
     *
     * @param beanType 服务 Bean 的真实用户类型
     * @return 不重复的服务类型列表
     */
    private static List<Class<?>> findServiceTypes(Class<?> beanType) {
        Objects.requireNonNull(beanType, "beanType must not be null");
        Set<Class<?>> serviceTypes = new LinkedHashSet<>();
        Class<?> currentType = beanType;
        while (currentType != null && currentType != Object.class) {
            if (findServiceAnnotation(currentType).isPresent()) {
                serviceTypes.add(currentType);
            }
            currentType = currentType.getSuperclass();
        }
        ClassUtils.getAllInterfacesForClassAsSet(beanType).stream()
                .filter(interfaceType -> findServiceAnnotation(interfaceType).isPresent())
                .sorted(Comparator.comparing(Class::getName)).forEach(serviceTypes::add);
        return List.copyOf(serviceTypes);
    }

    /**
     * 读取类型直接声明或通过组合注解声明的服务标记。
     *
     * @param serviceType 待检查类型
     * @return 存在服务标记时返回合成后的注解
     */
    private static Optional<GateWeaveService> findServiceAnnotation(Class<?> serviceType) {
        MergedAnnotation<GateWeaveService> annotation =
                MergedAnnotations.from(serviceType, MergedAnnotations.SearchStrategy.DIRECT)
                        .get(GateWeaveService.class);
        if (!annotation.isPresent()) {
            return Optional.empty();
        }
        return Optional.of(annotation.synthesize());
    }


    /**
     * 从单个已标记服务类型中提取服务定义。
     *
     * @param serviceType     已标记的服务类型
     * @param applicationName 服务所属应用名称
     * @return 完整服务元数据
     * @throws IllegalStateException 无法读取参数名称时抛出
     */
    public ServiceMetadata extractService(Class<?> serviceType, String applicationName) {
        GateWeaveService service = findServiceAnnotation(serviceType).orElseThrow();
        Map<Method, GateWeaveOperation> operationMethods = MethodIntrospector.selectMethods(serviceType,
                (MethodIntrospector.MetadataLookup<GateWeaveOperation>) method -> {
                    if (method.isBridge() || method.isSynthetic()) {
                        return null;
                    }
                    return AnnotatedElementUtils.findMergedAnnotation(method, GateWeaveOperation.class);
                });
        List<ServiceOperationMetadata> operations = operationMethods.entrySet().stream()
                .map(entry -> toOperationMetadata(serviceType, entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(ServiceOperationMetadata::operationId)).toList();
        return new ServiceMetadata(service.serviceId(), applicationName, service.protocol(), service.namespace(),
                service.version(), operations);
    }

    /**
     * 将一个已标记 Java 方法转换为服务操作元数据。
     *
     * @param serviceType 服务接口或服务实现类型
     * @param method      Java 服务方法
     * @param operation   方法上的操作注解
     * @return 服务操作元数据
     * @throws IllegalStateException 无法读取参数名称时抛出
     */
    private ServiceOperationMetadata toOperationMetadata(Class<?> serviceType, Method method,
            GateWeaveOperation operation) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        String[] parameterNames = this.parameterNameDiscoverer.getParameterNames(method);
        if (parameterNames == null || parameterNames.length != parameterTypes.length) {
            throw new IllegalStateException("Parameter names are not available for " + method.toGenericString());
        }
        List<ServiceParameterMetadata> parameters = new ArrayList<>(parameterTypes.length);
        for (int position = 0; position < parameterTypes.length; position++) {
            parameters.add(new ServiceParameterMetadata(position, parameterNames[position],
                    parameterTypes[position].getName()));
        }
        return new ServiceOperationMetadata(operation.operationId(), serviceType.getName(), method.getName(),
                parameters);
    }
}
