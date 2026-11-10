package com.rajesh.milansetu.exception;

import org.springframework.http.HttpStatus;

/** 409: someone else changed the same record at the same moment and retries were exhausted. */
public class ConcurrentUpdateException extends ApiException {
    public ConcurrentUpdateException(String message) {
        super(HttpStatus.CONFLICT, "CONCURRENT_UPDATE", message);
    }
}
