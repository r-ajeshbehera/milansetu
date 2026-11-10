package com.rajesh.milansetu.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record DeskRequest(@NotBlank @Size(max = 120) String name,
                          @NotNull @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
                          @NotNull @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
                          Long inChargeId, Boolean active) { }
