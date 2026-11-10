package com.rajesh.milansetu.service;

import com.rajesh.milansetu.dto.response.CaseResponse;
import com.rajesh.milansetu.dto.response.FoundReportResponse;
import com.rajesh.milansetu.dto.response.MissingReportResponse;
import com.rajesh.milansetu.entity.CaseFile;
import com.rajesh.milansetu.entity.Role;
import com.rajesh.milansetu.security.AppUserPrincipal;
import org.springframework.stereotype.Component;

/**
 * Builds role-appropriate case DTOs. No case DTO ever contains a guardian phone number or private member notes.
 * Families do not see who found the person or the volunteer's notes; staff do not see guardian identity.
 */
@Component
public class CaseMapper {

    public CaseResponse toResponse(CaseFile c, AppUserPrincipal viewer) {
        boolean family = viewer.role() == Role.FAMILY;
        String personName = c.getMissingReport() != null ? c.getMissingReport().getPersonName()
                : c.getMember() != null ? c.getMember().firstName() : null;
        FoundReportResponse found = c.getFoundReport() == null ? null : FoundReportResponse.from(c.getFoundReport());
        if (found != null && family) {
            found = new FoundReportResponse(found.id(), found.eventId(), found.deskId(), found.deskName(), found.source(),
                    found.registeredMember(), found.approxAge(), found.gender(), found.clothingColor(), null, null,
                    found.foundAt(), null, found.status());
        }
        return new CaseResponse(c.getId(), c.getEventId(), c.getStatus(), c.getMatchScore(), c.getMember() != null,
                personName,
                c.getMissingReport() == null ? null : MissingReportResponse.from(c.getMissingReport()),
                found,
                c.getAssignedDesk() == null ? null : c.getAssignedDesk().getId(),
                c.getAssignedDesk() == null ? null : c.getAssignedDesk().getName(),
                family || c.getAssignedOfficer() == null ? null : c.getAssignedOfficer().getFullName(),
                c.getOpenedAt(), c.getMatchedAt(), c.getReunitedAt(), c.getClosedAt(), c.getStaleFlaggedAt() != null);
    }
}
