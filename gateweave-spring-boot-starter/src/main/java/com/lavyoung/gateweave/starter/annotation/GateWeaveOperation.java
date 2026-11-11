package com.lavyoung.gateweave.starter.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记需要由 GateWeave Starter 采集的服务操作。
 *
 * <p>只有同时位于 GateWeave 服务类型中的标记方法才会被采集。
 * Java 方法名称和参数签名由元数据提取器读取，无需在注解中重复配置。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/11
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface GateWeaveOperation {

    /**
     * 返回服务内稳定且唯一的操作标识。
     *
     * <p>重载方法必须使用不同操作标识，避免仅依赖方法名称产生冲突。</p>
     *
     * @return 操作稳定标识
     */
    String operationId();
}
