package com.lavyoung.gateweave.contract.api;

import java.util.Objects;

/**
 * Stable error identity exposed by GateWeave APIs.
 *
 * @param code machine-readable error code
 * @param message safe human-readable message
 */
public record ErrorCode(String code, String message) {

    public ErrorCode {
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(message, "message must not be null");
    }
}
