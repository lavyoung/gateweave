package com.lavyoung.gateweave.contract;

import com.lavyoung.gateweave.contract.api.ErrorCode;
import com.lavyoung.gateweave.contract.api.Result;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResultTest {

    @Test
    void shouldCreateSuccessResult() {
        // Given
        var value = "ready";

        // When
        var result = Result.success(value);

        // Then
        assertThat(result.outcome()).isEqualTo(new Result.Success<>(value));
    }

    @Test
    void shouldCreateFailureResult() {
        // Given
        var error = new ErrorCode("GW-001", "route not found");

        // When
        var result = Result.failure(error);

        // Then
        assertThat(result.outcome()).isEqualTo(new Result.Failure<>(error));
    }
}
