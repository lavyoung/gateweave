package com.lavyoung.gateweave.contract.runtime;

import java.time.Instant;
import java.util.Objects;

/**
 * Runtime heartbeat reported by a gateway node.
 *
 * @param gatewayId gateway identity
 * @param nodeId runtime node identity
 * @param status lifecycle status
 * @param occurredAt heartbeat timestamp
 */
public record GatewayHeartbeat(
        String gatewayId,
        String nodeId,
        NodeStatus status,
        Instant occurredAt) {

    public GatewayHeartbeat {
        Objects.requireNonNull(gatewayId, "gatewayId must not be null");
        Objects.requireNonNull(nodeId, "nodeId must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }
}
