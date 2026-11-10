package com.rajesh.milansetu.dto.request;

import com.rajesh.milansetu.entity.ClothingColor;
import com.rajesh.milansetu.entity.Gender;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MissingReportRequest(
        @NotNull Long eventId,
        Long memberId,
        @Size(max = 120) String personName,
        @NotNull @Min(0) @Max(120) Integer approxAge,
        @NotNull Gender gender,
        @NotNull ClothingColor clothingColor,
        @NotNull @DecimalMin("-90") @DecimalMax("90") BigDecimal lastSeenLat,
        @NotNull @DecimalMin("-180") @DecimalMax("180") BigDecimal lastSeenLng,
        @NotNull @PastOrPresent LocalDateTime lastSeenAt) { }
