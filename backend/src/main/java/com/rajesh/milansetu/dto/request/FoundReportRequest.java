package com.rajesh.milansetu.dto.request;

import com.rajesh.milansetu.entity.ClothingColor;
import com.rajesh.milansetu.entity.Gender;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FoundReportRequest(
        @NotNull Long eventId,
        Long deskId,
        @NotNull @Min(0) @Max(120) Integer approxAge,
        @NotNull Gender gender,
        @NotNull ClothingColor clothingColor,
        @NotNull @DecimalMin("-90") @DecimalMax("90") BigDecimal foundLat,
        @NotNull @DecimalMin("-180") @DecimalMax("180") BigDecimal foundLng,
        @PastOrPresent LocalDateTime foundAt,
        @Size(max = 500) String safeNotes) { }
