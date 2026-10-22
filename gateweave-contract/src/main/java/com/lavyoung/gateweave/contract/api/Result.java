package com.lavyoung.gateweave.contract.api;

import java.util.Objects;

/**
 * Unified API result without nullable payloads or {@code Optional} fields.
 *
 * @param outcome successful value or failure details
 * @param <T> successful payload type
 */
public record Result<T>(Outcome<T> outcome) {

    public Result {
        Objects.requireNonNull(outcome, "outcome must not be null");
    }

    public static <T> Result<T> success(T value) {
        return new Result<>(new Success<>(value));
    }

    public static <T> Result<T> failure(ErrorCode error) {
        return new Result<>(new Failure<>(error));
    }

    public sealed interface Outcome<T> permits Success, Failure {
    }

    public record Success<T>(T value) implements Outcome<T> {

        public Success {
            Objects.requireNonNull(value, "value must not be null");
        }
    }

    public record Failure<T>(ErrorCode error) implements Outcome<T> {

        public Failure {
            Objects.requireNonNull(error, "error must not be null");
        }
    }
}
