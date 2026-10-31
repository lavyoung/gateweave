package com.lavyoung.gateweave.center.gateway.domain.exception;

import java.io.Serial;
import java.net.URI;
import java.util.Objects;

/**
 * 表示相同网关分组中的节点公布地址已被其他节点占用。
 *
 * <p>该异常只表达领域冲突，不暴露唯一索引名称、SQL 或 Spring DAO 异常。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/31
 */
public class GatewayNodeAddressConflictException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final URI advertisedAddress;

    /**
     * 创建节点地址冲突异常。
     *
     * @param advertisedAddress 已被占用的节点地址
     * @throws NullPointerException advertisedAddress 为 null 时抛出
     */
    public GatewayNodeAddressConflictException(URI advertisedAddress) {
        super("Gateway node address is already in use: "
                + Objects.requireNonNull(advertisedAddress, "advertisedAddress must not be null"));
        this.advertisedAddress = advertisedAddress;
    }

    /**
     * 返回发生冲突的节点地址。
     *
     * @return 节点公布地址
     */
    public URI advertisedAddress() {
        return this.advertisedAddress;
    }
}
