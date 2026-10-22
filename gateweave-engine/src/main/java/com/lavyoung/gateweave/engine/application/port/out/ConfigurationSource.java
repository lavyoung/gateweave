package com.lavyoung.gateweave.engine.application.port.out;

import com.lavyoung.gateweave.contract.configuration.GatewayConfiguration;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

/** Outbound port through which the data plane obtains published configuration. */
public interface ConfigurationSource {

    CompletionStage<Optional<GatewayConfiguration>> currentConfiguration(String gatewayId);
}
