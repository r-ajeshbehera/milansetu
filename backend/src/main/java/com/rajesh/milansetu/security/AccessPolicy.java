package com.rajesh.milansetu.security;

import com.rajesh.milansetu.entity.CaseFile;
import com.rajesh.milansetu.entity.EventStaffId;
import com.rajesh.milansetu.entity.Member;
import com.rajesh.milansetu.entity.Role;
import com.rajesh.milansetu.exception.ResourceNotFoundException;
import com.rajesh.milansetu.repository.EventStaffRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Every ownership / event-scope rule in one place.
 * Records the user may not see produce 404, so their existence is not revealed.
 */
@Component
public class AccessPolicy {
    private final EventStaffRepository staff;

    public AccessPolicy(EventStaffRepository staff) {
        this.staff = staff;
    }

    public boolean isStaffOf(AppUserPrincipal user, Long eventId) {
        return user.isAdmin() || staff.existsById(new EventStaffId(eventId, user.id()));
    }

    /** User must have one of the roles AND (be ADMIN or be assigned to the event). */
    public void assertEventRole(AppUserPrincipal user, Long eventId, Role... roles) {
        if (!Arrays.asList(roles).contains(user.role()) && !user.isAdmin()) {
            throw new AccessDeniedException("Role not allowed");
        }
        if (!isStaffOf(user, eventId)) throw new ResourceNotFoundException("Event", eventId);
    }

    /** Event ids a staff user works at (ADMIN: null = all events). */
    public List<Long> staffEvents(AppUserPrincipal user) {
        if (user.isAdmin()) return null;
        return staff.findByIdUserId(user.id()).stream().map(es -> es.getId().getEventId()).toList();
    }

    public Long homeDesk(AppUserPrincipal user, Long eventId) {
        return staff.findById(new EventStaffId(eventId, user.id())).map(es -> es.getDeskId()).orElse(null);
    }

    public void assertOwnsMember(AppUserPrincipal user, Member member) {
        if (!member.getGuardianId().equals(user.id())) throw new ResourceNotFoundException("Member", member.getId());
    }

    /** Families see only cases about their own members or their own missing reports; staff see their events. */
    public void assertCanViewCase(AppUserPrincipal user, CaseFile c) {
        if (user.role() == Role.FAMILY) {
            boolean ownsMember = c.getMember() != null && c.getMember().getGuardianId().equals(user.id());
            boolean ownsReport = c.getMissingReport() != null && c.getMissingReport().getReportedBy().getId().equals(user.id());
            if (!ownsMember && !ownsReport) throw new ResourceNotFoundException("Case", c.getId());
            return;
        }
        if (!isStaffOf(user, c.getEventId())) throw new ResourceNotFoundException("Case", c.getId());
    }
}
