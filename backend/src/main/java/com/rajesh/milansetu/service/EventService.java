package com.rajesh.milansetu.service;

import com.rajesh.milansetu.dto.request.DeskRequest;
import com.rajesh.milansetu.dto.request.EventRequest;
import com.rajesh.milansetu.dto.request.StaffRequest;
import com.rajesh.milansetu.dto.response.DeskResponse;
import com.rajesh.milansetu.dto.response.EventResponse;
import com.rajesh.milansetu.dto.response.StaffResponse;
import com.rajesh.milansetu.entity.*;
import com.rajesh.milansetu.exception.BusinessRuleException;
import com.rajesh.milansetu.exception.ResourceNotFoundException;
import com.rajesh.milansetu.repository.*;
import com.rajesh.milansetu.security.AppUserPrincipal;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class EventService {
    private final EventRepository events;
    private final HelpDeskRepository desks;
    private final EventStaffRepository staff;
    private final UserRepository users;
    private final AuditService audit;

    public EventService(EventRepository events, HelpDeskRepository desks, EventStaffRepository staff, UserRepository users,
                        AuditService audit) {
        this.events = events;
        this.desks = desks;
        this.staff = staff;
        this.users = users;
        this.audit = audit;
    }

    // ---- events ----------------------------------------------------------------------

    @Transactional
    public EventResponse create(EventRequest req, AppUserPrincipal admin) {
        validate(req);
        if (events.existsByNameIgnoreCase(req.name().trim())) throw new BusinessRuleException("EVENT_EXISTS", "Event name already used");
        Event e = new Event(req.name().trim(), req.city().trim(), req.startDate(), req.endDate(), users.getReferenceById(admin.id()));
        if (req.active() != null) e.setActive(req.active());
        e = events.save(e);
        audit.log(admin.id(), "EVENT_CREATED", "EVENT", e.getId(), null);
        return EventResponse.from(e);
    }

    @Transactional
    public EventResponse update(Long id, EventRequest req, AppUserPrincipal admin) {
        validate(req);
        Event e = find(id);
        e.setName(req.name().trim());
        e.setCity(req.city().trim());
        e.setStartDate(req.startDate());
        e.setEndDate(req.endDate());
        if (req.active() != null) e.setActive(req.active());
        audit.log(admin.id(), "EVENT_UPDATED", "EVENT", id, "active=" + e.isActive());
        return EventResponse.from(e);
    }

    @Transactional(readOnly = true)
    public List<EventResponse> list(boolean activeOnly) {
        List<Event> list = activeOnly ? events.findByActiveTrueOrderByStartDateDesc() : events.findAll(Sort.by(Sort.Direction.DESC, "startDate"));
        return list.stream().map(EventResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public EventResponse get(Long id) { return EventResponse.from(find(id)); }

    @Transactional(readOnly = true)
    public Event find(Long id) {
        return events.findById(id).orElseThrow(() -> new ResourceNotFoundException("Event", id));
    }

    @Transactional(readOnly = true)
    public Event requireActive(Long id) {
        Event e = find(id);
        if (!e.isActive()) throw new BusinessRuleException("EVENT_INACTIVE", "Event " + id + " is not active");
        return e;
    }

    // ---- help desks ------------------------------------------------------------------

    @Transactional
    public DeskResponse createDesk(Long eventId, DeskRequest req, AppUserPrincipal admin) {
        Event e = find(eventId);
        HelpDesk d = new HelpDesk(e, req.name().trim(), req.latitude(), req.longitude());
        if (req.active() != null) d.setActive(req.active());
        d = desks.save(d);
        if (req.inChargeId() != null) setInCharge(d, req.inChargeId(), admin);
        audit.log(admin.id(), "DESK_CREATED", "DESK", d.getId(), null);
        return DeskResponse.from(d);
    }

    @Transactional
    public DeskResponse updateDesk(Long deskId, DeskRequest req, AppUserPrincipal admin) {
        HelpDesk d = desks.findWithEvent(deskId).orElseThrow(() -> new ResourceNotFoundException("Desk", deskId));
        d.setName(req.name().trim());
        d.setLatitude(req.latitude());
        d.setLongitude(req.longitude());
        if (req.active() != null) d.setActive(req.active());
        if (req.inChargeId() != null) setInCharge(d, req.inChargeId(), admin);
        audit.log(admin.id(), "DESK_UPDATED", "DESK", deskId, null);
        return DeskResponse.from(d);
    }

    @Transactional(readOnly = true)
    public List<DeskResponse> desks(Long eventId) {
        find(eventId);
        return desks.findForEvent(eventId).stream().map(DeskResponse::from).toList();
    }

    /** The in-charge must be a DESK user; they are also added to the event staff automatically. */
    private void setInCharge(HelpDesk d, Long userId, AppUserPrincipal admin) {
        User u = users.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));
        if (u.getRole() != Role.DESK || !u.isActive()) throw new BusinessRuleException("INVALID_INCHARGE", "In-charge must be an active DESK user");
        d.setInCharge(u);
        EventStaff es = staff.findById(new EventStaffId(d.getEventId(), userId)).orElse(null);
        if (es == null) staff.save(new EventStaff(d.getEventId(), userId, d.getId(), admin.id()));
        else if (es.getDeskId() == null) es.setDeskId(d.getId());
    }

    // ---- staff -----------------------------------------------------------------------

    @Transactional
    public StaffResponse assignStaff(Long eventId, Long userId, StaffRequest req, AppUserPrincipal admin) {
        find(eventId);
        User u = users.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));
        if (!EnumSet.of(Role.VOLUNTEER, Role.DESK, Role.POLICE).contains(u.getRole())) {
            throw new BusinessRuleException("INVALID_STAFF_ROLE", "Only volunteers, desk users and police are assigned to events");
        }
        if (req.deskId() != null) {
            HelpDesk d = desks.findWithEvent(req.deskId()).orElseThrow(() -> new ResourceNotFoundException("Desk", req.deskId()));
            if (!d.getEventId().equals(eventId)) throw new BusinessRuleException("DESK_OTHER_EVENT", "Desk belongs to another event");
        }
        EventStaff es = staff.findById(new EventStaffId(eventId, userId))
                .orElseGet(() -> new EventStaff(eventId, userId, req.deskId(), admin.id()));
        es.setDeskId(req.deskId());
        staff.save(es);
        audit.log(admin.id(), "STAFF_ASSIGNED", "EVENT", eventId, "user=" + userId);
        return new StaffResponse(u.getId(), u.getFullName(), u.getRole(), es.getDeskId());
    }

    @Transactional
    public void removeStaff(Long eventId, Long userId, AppUserPrincipal admin) {
        staff.findById(new EventStaffId(eventId, userId)).ifPresent(staff::delete);
        audit.log(admin.id(), "STAFF_REMOVED", "EVENT", eventId, "user=" + userId);
    }

    @Transactional(readOnly = true)
    public List<StaffResponse> staff(Long eventId) {
        List<EventStaff> list = staff.findByIdEventId(eventId);
        Map<Long, User> byId = users.findAllById(list.stream().map(es -> es.getId().getUserId()).toList()).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return list.stream().map(es -> {
            User u = byId.get(es.getId().getUserId());
            return new StaffResponse(u.getId(), u.getFullName(), u.getRole(), es.getDeskId());
        }).toList();
    }

    private static void validate(EventRequest req) {
        if (req.endDate().isBefore(req.startDate())) throw new BusinessRuleException("INVALID_DATES", "End date is before start date");
    }
}
