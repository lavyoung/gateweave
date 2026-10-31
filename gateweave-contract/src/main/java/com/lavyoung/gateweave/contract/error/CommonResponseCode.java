package com.lavyoung.gateweave.contract.error;

/**
 * 定义所有限界上下文共享的基础响应码。
 *
 * <p>这里只保存与具体业务无关的结果。业务错误应由所属限界上下文分别定义，
 * 避免形成无限膨胀的全局枚举。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/31
 */
public enum CommonResponseCode implements ResponseCode {

    /**
     * 请求处理成功。
     */
    SUCCESS("SUCCESS", "common.success", "成功"),

    /**
     * 请求参数不符合接口约束。
     */
    PARAM_INVALID("COMMON-0001", "common.param.invalid", "请求参数无效"),

    /**
     * 请求体缺失或无法反序列化。
     */
    REQUEST_BODY_INVALID("COMMON-0002", "common.request.body.invalid", "请求体格式无效"),

    /**
     * 请求路径或目标资源不存在。
     */
    RESOURCE_NOT_FOUND("COMMON-0003", "common.resource.not-found", "请求的资源不存在"),

    /**
     * 未被业务规则识别的内部异常。
     */
    SYSTEM_ERROR("COMMON-9999", "system.error", "系统异常，请稍后重试");

    private final String code;
    private final String messageKey;
    private final String defaultMessage;

    /**
     * 创建公共响应码。
     *
     * @param code           稳定响应码
     * @param messageKey     国际化资源键
     * @param defaultMessage 默认响应文案
     */
    CommonResponseCode(String code, String messageKey, String defaultMessage) {
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
