package com.rajesh.milansetu.dto.response;

import java.time.LocalDateTime;

/** Deliberately contains no code: the OTP goes only to the guardian. */
public record OtpIssuedResponse(Long caseId, LocalDateTime expiresAt, String deliveredTo) { }
