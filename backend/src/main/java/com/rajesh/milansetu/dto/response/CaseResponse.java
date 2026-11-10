package com.rajesh.milansetu.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.rajesh.milansetu.entity.CaseStatus;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CaseResponse(Long id, Long eventId, CaseStatus status, Integer matchScore, boolean registeredMember,
                           String personName, MissingReportResponse missing, FoundReportResponse found,
                           Long assignedDeskId, String assignedDeskName, String assignedOfficer,
                           LocalDateTime openedAt, LocalDateTime matchedAt, LocalDateTime reunitedAt,
                           LocalDateTime closedAt, boolean stale) { }
