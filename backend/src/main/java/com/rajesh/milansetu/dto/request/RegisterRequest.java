package com.rajesh.milansetu.dto.request;

import jakarta.validation.constraints.*;

/** Public sign-up. Always creates a FAMILY account; there is deliberately no role field. */
public record RegisterRequest(
        @NotBlank @Size(max = 120) String fullName,
        @NotBlank @Email @Size(max = 160) String email,
        @NotBlank @Pattern(regexp = "^[6-9]\\d{9}$", message = "must be a 10-digit Indian mobile number") String phone,
        @NotBlank @Size(min = 8, max = 72) String password) { }
