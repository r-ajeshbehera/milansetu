package com.rajesh.milansetu.dto.request;

import jakarta.validation.constraints.Size;

/** foundReportId: required when confirming a scored suggestion; empty for a QR (registered member) case. */
public record ConfirmMatchRequest(Long foundReportId, @Size(max = 500) String note) { }
