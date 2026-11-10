package com.rajesh.milansetu.exception;

import org.springframework.http.HttpStatus;

/** 404. Also used for records outside the caller's district, so their existence is not revealed. */
public class ResourceNotFoundException extends ApiException {
    public ResourceNotFoundException(String what, Object id) {
        super(HttpStatus.NOT_FOUND, "NOT_FOUND", what + " " + id + " not found");
    }

    /** Fixed, generic message (used where details must not leak, e.g. invalid QR tokens). */
    public ResourceNotFoundException(String genericMessage) {
        super(HttpStatus.NOT_FOUND, "NOT_FOUND", genericMessage);
    }
}
