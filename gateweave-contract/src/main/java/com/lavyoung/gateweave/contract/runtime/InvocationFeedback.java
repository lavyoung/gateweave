package com.lavyoung.gateweave.contract.runtime;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Invocation observation reported by the data plane.
 *
 * @param requestId request identity
 * @param routeId matched route identity
 * @param statusCode protocol response status
 * @param duration invocation duration
 * @param occurredAt completion timestamp
 */
public record InvocationFeedback(
        String requestId,
        String routeId,
        int statusCode,
        Duration duration,
        Instant occurredAt) {

    public InvocationFeedback {
        Objects.requireNonNull(requestId, "requestId must not be null");
        Objects.requireNonNull(routeId, "routeId must not be null");
        Objects.requireNonNull(duration, "duration must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }
}
