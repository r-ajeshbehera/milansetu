package com.rajesh.milansetu.exception;

import org.springframework.http.HttpStatus;

/** 409: the action is not allowed in the record's current state (e.g. receive before dispatch). */
public class InvalidStateException extends ApiException {
    public InvalidStateException(String message) {
        super(HttpStatus.CONFLICT, "INVALID_STATE", message);
    }
}
