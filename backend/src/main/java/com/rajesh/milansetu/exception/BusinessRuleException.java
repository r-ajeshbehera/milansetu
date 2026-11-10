package com.rajesh.milansetu.exception;

import org.springframework.http.HttpStatus;

/** 422: the request is valid JSON but breaks a business rule. */
public class BusinessRuleException extends ApiException {
    public BusinessRuleException(String code, String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, code, message);
    }
}
