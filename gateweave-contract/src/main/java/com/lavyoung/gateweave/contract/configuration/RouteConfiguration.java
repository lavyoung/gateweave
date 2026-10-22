package com.lavyoung.gateweave.contract.configuration;

import java.util.Map;
import java.util.Objects;

/**
 * Published route representation consumed by gateway nodes.
 *
 * @param routeId stable route identity
 * @param path request path pattern
 * @param method protocol method
 * @param serviceId target service identity
 * @param attributes extension attributes
 */
public record RouteConfiguration(
        String routeId,
        String path,
        String method,
        String serviceId,
        Map<String, String> attributes) {

    public RouteConfiguration {
        Objects.requireNonNull(routeId, "routeId must not be null");
        Objects.requireNonNull(path, "path must not be null");
        Objects.requireNonNull(method, "method must not be null");
        Objects.requireNonNull(serviceId, "serviceId must not be null");
        attributes = Map.copyOf(attributes);
    }
}
