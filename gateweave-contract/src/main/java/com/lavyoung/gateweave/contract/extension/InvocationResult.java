package com.lavyoung.gateweave.contract.extension;

import java.util.Arrays;
import java.util.Map;

/**
 * Framework-neutral result returned by a service invoker.
 *
 * @param statusCode protocol response status
 * @param headers immutable response headers
 * @param payload response payload
 */
public record InvocationResult(int statusCode, Map<String, String> headers, byte[] payload) {

    public InvocationResult {
        headers = Map.copyOf(headers);
        payload = Arrays.copyOf(payload, payload.length);
    }

    @Override
    public byte[] payload() {
        return Arrays.copyOf(payload, payload.length);
    }
}
