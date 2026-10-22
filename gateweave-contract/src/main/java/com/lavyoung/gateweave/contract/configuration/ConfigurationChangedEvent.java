package com.lavyoung.gateweave.contract.configuration;

import java.time.Instant;
import java.util.Objects;

/**
 * Notification emitted after a configuration release.
 *
 * @param gatewayId target gateway identity
 * @param version published configuration version
 * @param occurredAt event timestamp
 */
public record ConfigurationChangedEvent(String gatewayId, long version, Instant occurredAt) {

    public ConfigurationChangedEvent {
        Objects.requireNonNull(gatewayId, "gatewayId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }
}
