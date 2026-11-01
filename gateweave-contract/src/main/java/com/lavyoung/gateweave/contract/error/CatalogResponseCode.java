package com.lavyoung.gateweave.contract.error;

/**
 * 定义服务目录限界上下文对外暴露的稳定响应码。
 *
 * <p>响应码只描述服务和操作注册中的业务失败，不包含 HTTP 状态码、
 * Spring 异常或数据库约束名称。不同入站协议负责完成各自的错误响应转换。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/01
 */
public enum CatalogResponseCode implements ResponseCode {

    /**
     * 注册服务操作时指定的服务不存在。
     */
    SERVICE_NOT_FOUND("CATALOG-0001", "catalog.service.not-found", "服务 {0} 不存在"),

    /**
     * 相同服务标识已经对应另一份服务定义。
     */
    SERVICE_DEFINITION_CONFLICT("CATALOG-0002", "catalog.service.definition-conflict", "服务 {0} 已存在且定义不一致"),

    /**
     * 相同操作标识或自然唯一键已经对应另一份操作定义。
     */
    OPERATION_DEFINITION_CONFLICT("CATALOG-0003", "catalog.operation.definition-conflict", "服务操作 {0} 已存在且定义不一致"),

    /**
     * 服务目录注册操作因未识别的持久化错误而失败。
     */
    REGISTRATION_FAILED("CATALOG-0004", "catalog.registration.failed", "服务目录注册失败");

    private final String code;
    private final String messageKey;
    private final String defaultMessage;

    /**
     * 创建服务目录响应码。
     *
     * @param code           稳定响应码
     * @param messageKey     国际化资源键
     * @param defaultMessage 国际化资源缺失时使用的默认文案
     */
    CatalogResponseCode(String code, String messageKey, String defaultMessage) {
        this.code = code;
        this.messageKey = messageKey;
        this.defaultMessage = defaultMessage;
    }

    @Override
    public String code() {
        return this.code;
    }

    @Override
    public String messageKey() {
        return this.messageKey;
    }

    @Override
    public String defaultMessage() {
        return this.defaultMessage;
    }
}
