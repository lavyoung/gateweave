package com.lavyoung.gateweave.engine.application.port.in;

import com.lavyoung.gateweave.contract.extension.InvocationRequest;
import com.lavyoung.gateweave.contract.extension.InvocationResult;

import java.util.concurrent.CompletionStage;

/** Inbound use case for executing a gateway invocation. */
public interface HandleInvocationUseCase {

    CompletionStage<InvocationResult> handle(InvocationRequest request);
}
