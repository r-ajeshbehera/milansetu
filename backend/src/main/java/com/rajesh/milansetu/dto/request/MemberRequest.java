package com.rajesh.milansetu.dto.request;

import com.rajesh.milansetu.entity.Gender;
import jakarta.validation.constraints.*;

public record MemberRequest(@NotNull Long eventId, @NotBlank @Size(max = 120) String fullName,
                            @NotNull @Min(0) @Max(120) Integer age, @NotNull Gender gender,
                            @Size(max = 500) String notes) { }
