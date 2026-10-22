package com.lavyoung.gateweave.contract.extension;

import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

/**
 * Framework-neutral invocation request passed to service invokers.
 *
 * @param requestId request identity
 * @param routeId matched route identity
 * @param target target resource URI
 * @param method protocol method
 * @param headers immutable headers
 * @param payload request payload
 */
public record InvocationRequest(
        String requestId,
        String routeId,
        URI target,
        String method,
        Map<String, String> headers,
        byte[] payload) {

    public InvocationRequest {
        Objects.requireNonNull(requestId, "requestId must not be null");
        Objects.requireNonNull(routeId, "routeId must not be null");
        Objects.requireNonNull(target, "target must not be null");
        Objects.requireNonNull(method, "method must not be null");
        headers = Map.copyOf(headers);
        payload = Arrays.copyOf(payload, payload.length);
    }

    @Override
    public byte[] payload() {
        return Arrays.copyOf(payload, payload.length);
    }
}
