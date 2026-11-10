package com.rajesh.milansetu.controller;

import com.rajesh.milansetu.dto.request.*;
import com.rajesh.milansetu.dto.response.CaseResponse;
import com.rajesh.milansetu.dto.response.OtpIssuedResponse;
import com.rajesh.milansetu.dto.response.TimelineEntry;
import com.rajesh.milansetu.entity.CaseStatus;
import com.rajesh.milansetu.security.CurrentUser;
import com.rajesh.milansetu.service.CaseService;
import com.rajesh.milansetu.service.OtpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/cases")
@Tag(name = "Cases, matching confirmation and handover")
public class CaseController {
    private final CaseService cases;
    private final OtpService otp;

    public CaseController(CaseService cases, OtpService otp) {
        this.cases = cases;
        this.otp = otp;
    }

    @GetMapping
    public Page<CaseResponse> list(@RequestParam(required = false) Long eventId,
                                   @RequestParam(required = false) CaseStatus status,
                                   @RequestParam(required = false) Long deskId,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                   @PageableDefault(size = 20, sort = "openedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return cases.list(eventId, status, deskId, from, to, Pages.capped(pageable), CurrentUser.get());
    }

    @GetMapping("/{id}")
    public CaseResponse get(@PathVariable Long id) {
        return cases.get(id, CurrentUser.get());
    }

    @GetMapping("/{id}/timeline")
    public List<TimelineEntry> timeline(@PathVariable Long id) {
        return cases.timeline(id, CurrentUser.get());
    }

    @PostMapping("/{id}/confirm-match")
    @PreAuthorize("hasRole('DESK')")
    @Operation(summary = "Desk confirms a match (manual). Issues an OTP to the guardian.")
    public CaseResponse confirmMatch(@PathVariable Long id, @Valid @RequestBody ConfirmMatchRequest req) {
        return cases.confirmMatch(id, req, CurrentUser.get());
    }

    @PostMapping("/{id}/reject-match")
    @PreAuthorize("hasRole('DESK')")
    public CaseResponse rejectMatch(@PathVariable Long id, @Valid @RequestBody(required = false) NoteRequest req) {
        return cases.rejectMatch(id, req == null ? null : req.note(), CurrentUser.get());
    }

    @PostMapping("/{id}/arrived")
    @PreAuthorize("hasRole('DESK')")
    public CaseResponse arrived(@PathVariable Long id) {
        return cases.arrived(id, CurrentUser.get());
    }

    @PostMapping("/{id}/otp")
    @PreAuthorize("hasRole('DESK')")
    @Operation(summary = "Issue a new OTP (supersedes the old one). Response contains only the expiry time.")
    public OtpIssuedResponse issueOtp(@PathVariable Long id) {
        return otp.issue(id, CurrentUser.get());
    }

    @PostMapping("/{id}/handover")
    @PreAuthorize("hasRole('DESK')")
    @Operation(summary = "Verify the guardian's OTP; the ONLY way a case becomes REUNITED.")
    public CaseResponse handover(@PathVariable Long id, @Valid @RequestBody HandoverRequest req) {
        return otp.verifyAndHandover(id, req.otp(), CurrentUser.get());
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('POLICE','ADMIN')")
    public CaseResponse assign(@PathVariable Long id, @RequestBody AssignRequest req) {
        return cases.assign(id, req, CurrentUser.get());
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('POLICE','ADMIN','FAMILY')")
    public CaseResponse close(@PathVariable Long id, @Valid @RequestBody CloseRequest req) {
        return cases.close(id, req.note(), CurrentUser.get());
    }
}
