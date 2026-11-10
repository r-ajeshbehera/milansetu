package com.rajesh.milansetu.dto.request;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record EventRequest(@NotBlank @Size(max = 120) String name, @NotBlank @Size(max = 80) String city,
                           @NotNull LocalDate startDate, @NotNull LocalDate endDate, Boolean active) { }
