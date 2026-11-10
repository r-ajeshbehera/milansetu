package com.rajesh.milansetu.dto.response;

import com.rajesh.milansetu.entity.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Staff view of a found report. registeredMember says only WHETHER a wristband was scanned. */
public record FoundReportResponse(Long id, Long eventId, Long deskId, String deskName, FoundSource source,
                                  boolean registeredMember, int approxAge, Gender gender, ClothingColor clothingColor,
                                  BigDecimal foundLat, BigDecimal foundLng, LocalDateTime foundAt, String safeNotes,
                                  ReportStatus status) {
    public static FoundReportResponse from(FoundReport f) {
        return new FoundReportResponse(f.getId(), f.getEventId(), f.getDesk().getId(), f.getDesk().getName(), f.getSource(),
                f.getMember() != null, f.getApproxAge(), f.getGender(), f.getClothingColor(), f.getFoundLat(),
                f.getFoundLng(), f.getFoundAt(), f.getSafeNotes(), f.getStatus());
    }
}
