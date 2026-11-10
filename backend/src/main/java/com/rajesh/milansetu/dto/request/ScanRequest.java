package com.rajesh.milansetu.dto.request;

import com.rajesh.milansetu.entity.ClothingColor;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

/** All optional: GPS from the phone (if permitted) and the clothing colour the volunteer sees. */
public record ScanRequest(@DecimalMin("-90") @DecimalMax("90") BigDecimal lat,
                          @DecimalMin("-180") @DecimalMax("180") BigDecimal lng,
                          ClothingColor clothingColor) { }
