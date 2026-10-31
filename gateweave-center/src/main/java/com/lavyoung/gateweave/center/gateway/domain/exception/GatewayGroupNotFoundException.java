package com.lavyoung.gateweave.center.gateway.domain.exception;

import java.io.Serial;
import java.util.Objects;

/**
 * 表示节点注册时指定的网关分组不存在。
 *
 * <p>该异常表达稳定的领域失败原因，不包含 HTTP 状态码、数据库异常或国际化文案。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/31
 */
public class GatewayGroupNotFoundException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String gatewayGroupId;

    /**
     * 创建网关分组不存在异常。
     *
     * @param gatewayGroupId 不存在的网关分组标识
     * @throws NullPointerException gatewayGroupId 为 null 时抛出
     */
    public GatewayGroupNotFoundException(String gatewayGroupId) {
        super("Gateway group does not exist: "
                + Objects.requireNonNull(gatewayGroupId, "gatewayGroupId must not be null"));
        this.gatewayGroupId = gatewayGroupId;
    }

    /**
     * 返回不存在的网关分组标识。
     *
     * @return 网关分组标识
     */
    public String gatewayGroupId() {
        return this.gatewayGroupId;
    }
}
