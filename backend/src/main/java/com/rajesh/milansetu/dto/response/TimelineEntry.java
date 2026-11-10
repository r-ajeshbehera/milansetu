package com.rajesh.milansetu.dto.response;

import com.rajesh.milansetu.entity.CaseStatus;
import java.time.LocalDateTime;

public record TimelineEntry(Long caseId, CaseStatus fromStatus, CaseStatus status, String note, String changedBy,
                            LocalDateTime changedAt) { }
