package com.lavyoung.gateweave.starter.registration;

import com.lavyoung.gateweave.starter.metadata.ServiceMetadata;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 收集当前 Spring 应用中已经发现的服务元数据。
 *
 * <p>该注册表作为元数据消费者接入 Bean 后置处理器，并为后续启动注册流程提供
 * 排序后的不可变快照。它只管理当前进程内的采集结果，不执行 HTTP 请求。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/13
 */
public final class ServiceMetadataRegistry implements Consumer<ServiceMetadata> {

    private final Map<String, ServiceMetadata> metadataByServiceId = new ConcurrentHashMap<>();

    /**
     * 保存一份服务元数据。
     *
     * <p>同一服务重复提交完全一致的定义时保持幂等；同一标识对应不同定义时立即失败，
     * 避免应用启动后向控制面发送不确定的服务描述。</p>
     *
     * @param metadata 服务元数据
     * @throws NullPointerException  metadata 为 null 时抛出
     * @throws IllegalStateException 同一服务标识对应不同定义时抛出
     */
    @Override
    public void accept(ServiceMetadata metadata) {
        Objects.requireNonNull(metadata, "metadata must not be null");
        ServiceMetadata absent = this.metadataByServiceId.putIfAbsent(metadata.serviceId(), metadata);
        if (absent != null && !Objects.equals(absent, metadata)) {
            throw new IllegalStateException(
                    "Conflicting service metadata discovered for serviceId: " + metadata.serviceId());
        }
    }

    /**
     * 返回当前已经采集的服务元数据快照。
     *
     * @return 按服务标识排序的不可变元数据列表
     */
    public List<ServiceMetadata> snapshot() {
        return this.metadataByServiceId.values().stream().sorted(Comparator.comparing(ServiceMetadata::serviceId))
                .toList();
    }
}
