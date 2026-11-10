package com.rajesh.milansetu.entity;

import java.util.EnumSet;
import java.util.Set;

/**
 * REPORTED -> MATCHED -> AT_DESK -> REUNITED -> CLOSED.
 * MATCHED -> REPORTED undoes a wrong match. Any open status can be CLOSED by police.
 * REUNITED is deliberately NOT reachable through canMoveTo(): only OtpService.verifyAndHandover sets it.
 */
public enum CaseStatus {
    REPORTED, MATCHED, AT_DESK, REUNITED, CLOSED;

    public Set<CaseStatus> allowedNext() {
        return switch (this) {
            case REPORTED -> EnumSet.of(MATCHED, CLOSED);
            case MATCHED -> EnumSet.of(AT_DESK, REPORTED, CLOSED);
            case AT_DESK -> EnumSet.of(CLOSED);
            case REUNITED -> EnumSet.of(CLOSED);
            case CLOSED -> EnumSet.noneOf(CaseStatus.class);
        };
    }

    public boolean canMoveTo(CaseStatus next) {
        return allowedNext().contains(next);
    }

    public boolean isOpen() {
        return this != REUNITED && this != CLOSED;
    }
}
