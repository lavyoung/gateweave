package com.lavyoung.gateweave.contract.configuration;

import java.util.List;
import java.util.Objects;

/**
 * Versioned configuration snapshot published to a gateway node.
 *
 * @param gatewayId target gateway identity
 * @param version monotonically increasing configuration version
 * @param routes immutable route definitions
 */
public record GatewayConfiguration(
        String gatewayId,
        long version,
        List<RouteConfiguration> routes) {

    public GatewayConfiguration {
        Objects.requireNonNull(gatewayId, "gatewayId must not be null");
        routes = List.copyOf(routes);
    }
}
