package com.lavyoung.gateweave.contract.extension;

import java.util.concurrent.CompletionStage;

/** Invokes an external resource without exposing vendor-specific client types. */
public interface ServiceInvoker extends GatewayPlugin {

    boolean supports(InvocationRequest request);

    CompletionStage<InvocationResult> invoke(InvocationRequest request);
}
