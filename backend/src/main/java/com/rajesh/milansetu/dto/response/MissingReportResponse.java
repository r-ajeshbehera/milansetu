package com.rajesh.milansetu.dto.response;

import com.rajesh.milansetu.entity.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MissingReportResponse(Long id, Long eventId, Long memberId, String personName, int approxAge, Gender gender,
                                    ClothingColor clothingColor, BigDecimal lastSeenLat, BigDecimal lastSeenLng,
                                    LocalDateTime lastSeenAt, ReportStatus status) {
    public static MissingReportResponse from(MissingReport r) {
        return new MissingReportResponse(r.getId(), r.getEventId(), r.getMember() == null ? null : r.getMember().getId(),
                r.getPersonName(), r.getApproxAge(), r.getGender(), r.getClothingColor(), r.getLastSeenLat(),
                r.getLastSeenLng(), r.getLastSeenAt(), r.getStatus());
    }
}
