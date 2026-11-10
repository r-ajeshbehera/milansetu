package com.rajesh.milansetu.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record HandoverRequest(@NotBlank @Pattern(regexp = "\\d{6}", message = "must be 6 digits") String otp) { }
