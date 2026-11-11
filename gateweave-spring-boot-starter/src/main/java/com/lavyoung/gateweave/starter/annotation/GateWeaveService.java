package com.lavyoung.gateweave.starter.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记需要由 GateWeave Starter 采集元数据的服务类型。
 *
 * <p>服务提供方使用该注解声明稳定的服务身份、调用协议、命名空间和版本。
 * Starter 只采集这些服务定义，不在本章发送注册请求或生成路由规则。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/11
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface GateWeaveService {

    /**
     * 返回服务稳定标识。
     *
     * <p>该标识在应用重启和服务实现调整后仍应保持不变。</p>
     *
     * @return 服务稳定标识
     */
    String serviceId();

    /**
     * 返回服务调用协议。
     *
     * @return 服务调用协议
     */
    String protocol() default "dubbo";

    /**
     * 返回服务命名空间。
     *
     * @return 服务命名空间，不使用命名空间时返回空字符串
     */
    String namespace() default "";

    /**
     * 返回服务版本。
     *
     * @return 服务版本，不区分版本时返回空字符串
     */
    String version() default "";
}
