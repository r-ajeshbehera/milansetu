package com.rajesh.milansetu.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(@Size(max = 120) String fullName, @Pattern(regexp = "^[6-9]\\d{9}$") String phone) { }
