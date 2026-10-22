package com.lavyoung.gateweave.contract.extension;

import java.util.concurrent.CompletionStage;

/** Advances request processing to the next configured gateway filter. */
@FunctionalInterface
public interface GatewayFilterChain {

    CompletionStage<InvocationResult> proceed(InvocationRequest request);
}
