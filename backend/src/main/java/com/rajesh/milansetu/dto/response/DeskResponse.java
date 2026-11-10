package com.rajesh.milansetu.dto.response;

import com.rajesh.milansetu.entity.HelpDesk;
import java.math.BigDecimal;

/** Public-safe desk info (no contact details). */
public record DeskResponse(Long id, Long eventId, String name, BigDecimal latitude, BigDecimal longitude,
                           Long inChargeId, String inChargeName, boolean active) {
    public static DeskResponse from(HelpDesk d) {
        return new DeskResponse(d.getId(), d.getEventId(), d.getName(), d.getLatitude(), d.getLongitude(),
                d.getInCharge() == null ? null : d.getInCharge().getId(),
                d.getInCharge() == null ? null : d.getInCharge().getFullName(), d.isActive());
    }
}
