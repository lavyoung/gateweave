package com.lavyoung.gateweave.contract.extension;

import java.util.concurrent.CompletionStage;

/** Ordered extension point for gateway request processing. */
public interface GatewayFilter extends GatewayPlugin {

    int order();

    CompletionStage<InvocationResult> filter(InvocationRequest request, GatewayFilterChain chain);
}
