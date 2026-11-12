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

        // 应用名称会进入最终注册元数据，先去除配置值首尾可能携带的空白字符。
        String checkedApplicationName =
                Objects.requireNonNull(applicationName, "applicationName must not be null").strip();
        if (checkedApplicationName.isEmpty()) {
            throw new IllegalArgumentException("applicationName must not be blank");
        }

        // 一个 Bean 可能自身带有服务注解，也可能实现多个带注解的服务接口，因此这里返回服务列表。
        List<ServiceMetadata> services = findServiceTypes(beanType).stream()
                .map(serviceType -> extractService(serviceType, checkedApplicationName))
                // 反射返回顺序不属于稳定契约，按业务标识排序后，注册结果和测试结果才可重复。
                .sorted(Comparator.comparing(ServiceMetadata::serviceId)).toList();

        // 同一个 Bean 暴露的多个服务必须拥有不同 serviceId，否则控制面无法区分服务身份。
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

        // LinkedHashSet 同时完成类型去重和发现顺序保留，避免同一接口被多条继承路径重复采集。
        Set<Class<?>> serviceTypes = new LinkedHashSet<>();

        // 先沿父类链向上检查，兼容服务注解直接标记在实现类或其业务父类上的情况。
        Class<?> currentType = beanType;
        while (currentType != null && currentType != Object.class) {
            if (findServiceAnnotation(currentType).isPresent()) {
                serviceTypes.add(currentType);
            }
            currentType = currentType.getSuperclass();
        }

        // 再检查所有直接和间接实现的接口，兼容常见的“注解声明在服务接口上”模式。
        ClassUtils.getAllInterfacesForClassAsSet(beanType).stream()
                .filter(interfaceType -> findServiceAnnotation(interfaceType).isPresent())
                // 接口发现顺序可能受 JVM 和代理实现影响，按全限定名排序以获得稳定结果。
                .sorted(Comparator.comparing(Class::getName)).forEach(serviceTypes::add);

        // 返回不可变快照，防止调用方修改本次类型发现结果。
        return List.copyOf(serviceTypes);
    }

    /**
     * 读取类型直接声明或通过组合注解声明的服务标记。
     *
     * @param serviceType 待检查类型
     * @return 存在服务标记时返回合成后的注解
     */
    private static Optional<GateWeaveService> findServiceAnnotation(Class<?> serviceType) {
        // DIRECT 只检查当前类型；父类和接口的遍历由 findServiceTypes 统一控制，避免隐式重复搜索。
        MergedAnnotation<GateWeaveService> annotation =
                MergedAnnotations.from(serviceType, MergedAnnotations.SearchStrategy.DIRECT)
                        .get(GateWeaveService.class);
        if (!annotation.isPresent()) {
            return Optional.empty();
        }

        // Spring 的合成注解会把读取到的属性值转换成可直接调用的 GateWeaveService 实例。
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
        // 该方法只接受已经识别为服务的类型；注解不存在说明调用链使用错误，直接失败。
        GateWeaveService service = findServiceAnnotation(serviceType).orElseThrow();

        // MethodIntrospector 一次遍历服务方法，并把方法与其 GateWeaveOperation 注解配对保存。
        Map<Method, GateWeaveOperation> operationMethods = MethodIntrospector.selectMethods(serviceType,
                (MethodIntrospector.MetadataLookup<GateWeaveOperation>) method -> {
                    // 泛型擦除可能生成桥接方法，编译器也可能生成辅助方法，它们都不是真实服务操作。
                    if (method.isBridge() || method.isSynthetic()) {
                        return null;
                    }
                    // 支持 Spring 的合并注解读取方式，未标记的方法返回 null 后不会进入结果 Map。
                    return AnnotatedElementUtils.findMergedAnnotation(method, GateWeaveOperation.class);
                });

        // 将 Java 反射模型转换为 Starter 自有模型，避免 Method 等技术对象继续向外传播。
        List<ServiceOperationMetadata> operations = operationMethods.entrySet().stream()
                .map(entry -> toOperationMetadata(serviceType, entry.getKey(), entry.getValue()))
                // 方法反射顺序同样不稳定，使用业务 operationId 固定输出顺序。
                .sorted(Comparator.comparing(ServiceOperationMetadata::operationId)).toList();

        // 类型注解提供服务身份，反射结果提供操作列表，二者在这里汇总成完整服务元数据。
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
        // 参数类型始终可以从方法签名读取，并严格保持 Java 方法中的声明顺序。
        Class<?>[] parameterTypes = method.getParameterTypes();

        // 参数名称依赖 -parameters 编译信息，交给 Spring 发现器统一读取并兼容不同发现策略。
        String[] parameterNames = this.parameterNameDiscoverer.getParameterNames(method);
        if (parameterNames == null || parameterNames.length != parameterTypes.length) {
            throw new IllegalStateException("Parameter names are not available for " + method.toGenericString());
        }

        // 使用数组下标作为稳定位置，把同一下标的参数名称和参数类型组合成参数元数据。
        List<ServiceParameterMetadata> parameters = new ArrayList<>(parameterTypes.length);
        for (int position = 0; position < parameterTypes.length; position++) {
            parameters.add(new ServiceParameterMetadata(position, parameterNames[position],
                    parameterTypes[position].getName()));
        }

        // operationId 来自注解；接口名、Java 方法名和参数列表来自真实方法签名。
        return new ServiceOperationMetadata(operation.operationId(), serviceType.getName(), method.getName(),
                parameters);
    }
}
