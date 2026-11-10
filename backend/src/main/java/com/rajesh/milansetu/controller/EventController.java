package com.rajesh.milansetu.controller;

import com.rajesh.milansetu.dto.request.DeskRequest;
import com.rajesh.milansetu.dto.request.EventRequest;
import com.rajesh.milansetu.dto.request.StaffRequest;
import com.rajesh.milansetu.dto.response.DeskResponse;
import com.rajesh.milansetu.dto.response.EventResponse;
import com.rajesh.milansetu.dto.response.StaffResponse;
import com.rajesh.milansetu.security.CurrentUser;
import com.rajesh.milansetu.service.EventService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Events, desks and staff")
public class EventController {
    private final EventService events;

    public EventController(EventService events) {
        this.events = events;
    }

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public EventResponse create(@Valid @RequestBody EventRequest req) {
        return events.create(req, CurrentUser.get());
    }

    @GetMapping("/events")
    public List<EventResponse> list(@RequestParam(defaultValue = "false") boolean active) {
        return events.list(active);
    }

    @GetMapping("/events/{id}")
    public EventResponse get(@PathVariable Long id) {
        return events.get(id);
    }

    @PutMapping("/events/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public EventResponse update(@PathVariable Long id, @Valid @RequestBody EventRequest req) {
        return events.update(id, req, CurrentUser.get());
    }

    @PostMapping("/events/{id}/desks")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public DeskResponse createDesk(@PathVariable Long id, @Valid @RequestBody DeskRequest req) {
        return events.createDesk(id, req, CurrentUser.get());
    }

    @GetMapping("/events/{id}/desks")
    public List<DeskResponse> desks(@PathVariable Long id) {
        return events.desks(id);
    }

    @PutMapping("/desks/{deskId}")
    @PreAuthorize("hasRole('ADMIN')")
    public DeskResponse updateDesk(@PathVariable Long deskId, @Valid @RequestBody DeskRequest req) {
        return events.updateDesk(deskId, req, CurrentUser.get());
    }

    @GetMapping("/events/{id}/staff")
    @PreAuthorize("hasRole('ADMIN')")
    public List<StaffResponse> staff(@PathVariable Long id) {
        return events.staff(id);
    }

    @PutMapping("/events/{id}/staff/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public StaffResponse assign(@PathVariable Long id, @PathVariable Long userId, @RequestBody StaffRequest req) {
        return events.assignStaff(id, userId, req, CurrentUser.get());
    }

    @DeleteMapping("/events/{id}/staff/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> remove(@PathVariable Long id, @PathVariable Long userId) {
        events.removeStaff(id, userId, CurrentUser.get());
        return ResponseEntity.noContent().build();
    }
}
