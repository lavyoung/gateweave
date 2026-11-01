package com.lavyoung.gateweave.center.gateway.domain.exception;

import com.lavyoung.gateweave.center.shared.exception.BusinessException;
import com.lavyoung.gateweave.contract.error.GatewayResponseCode;

import java.io.Serial;
import java.util.Objects;

/**
 * 表示节点注册时指定的网关分组不存在。
 *
 * <p>异常通过稳定响应码表达领域失败原因，不包含 HTTP 状态码、数据库异常或 Web 技术对象。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/01
 */
public class GatewayGroupNotFoundException extends BusinessException {

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
        super(GatewayResponseCode.GATEWAY_GROUP_NOT_FOUND,
                Objects.requireNonNull(gatewayGroupId, "gatewayGroupId must not be null"));
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
