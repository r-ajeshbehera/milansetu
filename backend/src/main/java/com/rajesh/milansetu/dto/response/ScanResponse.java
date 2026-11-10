package com.rajesh.milansetu.dto.response;

import com.rajesh.milansetu.entity.Gender;

/**
 * The ONLY data a volunteer gets after a scan: first name, age, gender, where to take the person.
 * No guardian name, phone, notes or member id.
 */
public record ScanResponse(Long caseId, boolean newCase, String firstName, int approxAge, Gender gender,
                           DeskInfo desk, String instructions) {
    public record DeskInfo(Long id, String name, java.math.BigDecimal latitude, java.math.BigDecimal longitude, Double distanceKm) { }
}
