package com.rajesh.milansetu.exception;

import org.springframework.http.HttpStatus;

/**
 * OTP verification failed. Thrown from a transaction marked noRollbackFor this class, so the incremented
 * attempt counter / LOCKED / EXPIRED status is still saved.
 */
public class OtpRejectedException extends ApiException {
    private final int attemptsLeft;

    public OtpRejectedException(HttpStatus status, String code, String message, int attemptsLeft) {
        super(status, code, message);
        this.attemptsLeft = attemptsLeft;
    }

    public int getAttemptsLeft() { return attemptsLeft; }
}
