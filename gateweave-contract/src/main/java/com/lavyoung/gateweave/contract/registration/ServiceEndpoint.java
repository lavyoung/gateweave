package com.lavyoung.gateweave.contract.registration;

import java.util.Objects;

/**
 * A service endpoint reported by an onboarding client.
 *
 * @param operationId stable operation identity
 * @param path externally routable path
 * @param method protocol method such as GET or POST
 */
public record ServiceEndpoint(String operationId, String path, String method) {

    public ServiceEndpoint {
        Objects.requireNonNull(operationId, "operationId must not be null");
        Objects.requireNonNull(path, "path must not be null");
        Objects.requireNonNull(method, "method must not be null");
    }
}
