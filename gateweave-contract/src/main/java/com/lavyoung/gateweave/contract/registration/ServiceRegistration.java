package com.lavyoung.gateweave.contract.registration;

import java.net.URI;
import java.util.List;
import java.util.Objects;

/**
 * Registration request sent by a service instance.
 *
 * @param serviceId stable service identity
 * @param instanceId unique runtime instance identity
 * @param baseUri instance base URI
 * @param endpoints immutable endpoint metadata
 */
public record ServiceRegistration(
        String serviceId,
        String instanceId,
        URI baseUri,
        List<ServiceEndpoint> endpoints) {

    public ServiceRegistration {
        Objects.requireNonNull(serviceId, "serviceId must not be null");
        Objects.requireNonNull(instanceId, "instanceId must not be null");
        Objects.requireNonNull(baseUri, "baseUri must not be null");
        endpoints = List.copyOf(endpoints);
    }
}
