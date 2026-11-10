package com.rajesh.milansetu.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.Map;

/** The single error format returned by every endpoint. */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiError(Instant timestamp, int status, String code, String message, String path,
                       Map<String, String> fieldErrors, Map<String, Object> details) {

    public static ApiError of(int status, String code, String message, String path) {
        return new ApiError(Instant.now(), status, code, message, path, Map.of(), Map.of());
    }
}
