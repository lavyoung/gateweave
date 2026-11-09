package com.lavyoung.gateweave.contract.error;

/**
 * 定义网关集群限界上下文对外暴露的稳定响应码。
 *
 * <p>枚举只描述跨进程可识别的业务结果，不包含 HTTP 状态码。
 * Web 适配器负责将这些响应码分别映射为 404、409 或 500。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/31
 */
public enum GatewayResponseCode implements ResponseCode {

    /**
     * 节点注册时指定的网关分组不存在。
     */
    GATEWAY_GROUP_NOT_FOUND("GATEWAY-0001", "gateway.group.not-found", "网关分组 {0} 不存在"),

    /**
     * 相同网关分组内的节点地址已经被其他节点占用。
     */
    GATEWAY_NODE_ADDRESS_CONFLICT("GATEWAY-0002", "gateway.node.address-conflict", "网关节点地址 {0} 已被占用"),

    /**
     * 节点注册操作未能完成。
     */
    GATEWAY_NODE_REGISTRATION_FAILED("GATEWAY-0003", "gateway.node.registration-failed", "网关节点注册失败"),

    /**
     * 节点心跳携带的租约已经失效。
     */
    GATEWAY_NODE_LEASE_REJECTED("GATEWAY-0004", "gateway.node.lease-rejected", "网关节点 {0} 的租约已失效");

    private final String code;
    private final String messageKey;
    private final String defaultMessage;

    /**
     * 创建网关业务响应码。
     *
     * @param code           稳定响应码
     * @param messageKey     国际化资源键
     * @param defaultMessage 默认响应文案
     */
    GatewayResponseCode(String code, String messageKey, String defaultMessage) {
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
