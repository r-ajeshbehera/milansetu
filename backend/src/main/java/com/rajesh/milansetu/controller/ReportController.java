package com.rajesh.milansetu.controller;

import com.rajesh.milansetu.dto.request.FoundReportRequest;
import com.rajesh.milansetu.dto.request.MissingReportRequest;
import com.rajesh.milansetu.dto.response.CaseResponse;
import com.rajesh.milansetu.dto.response.FoundReportResponse;
import com.rajesh.milansetu.dto.response.MatchSuggestion;
import com.rajesh.milansetu.dto.response.MissingReportResponse;
import com.rajesh.milansetu.entity.ReportStatus;
import com.rajesh.milansetu.security.CurrentUser;
import com.rajesh.milansetu.service.MatchingService;
import com.rajesh.milansetu.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@Tag(name = "Missing and found reports")
public class ReportController {
    private final ReportService reports;
    private final MatchingService matching;

    public ReportController(ReportService reports, MatchingService matching) {
        this.reports = reports;
        this.matching = matching;
    }

    @PostMapping("/missing")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('FAMILY')")
    public CaseResponse missing(@Valid @RequestBody MissingReportRequest req) {
        return reports.reportMissing(req, CurrentUser.get());
    }

    @GetMapping("/missing")
    @PreAuthorize("hasAnyRole('FAMILY','DESK','POLICE','ADMIN')")
    public Page<MissingReportResponse> listMissing(@RequestParam(required = false) ReportStatus status,
                                                   @PageableDefault(size = 20, sort = "lastSeenAt") Pageable pageable) {
        return reports.listMissing(status, Pages.capped(pageable), CurrentUser.get());
    }

    @PostMapping("/found")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('VOLUNTEER','DESK')")
    public CaseResponse found(@Valid @RequestBody FoundReportRequest req) {
        return reports.reportFound(req, CurrentUser.get());
    }

    @GetMapping("/found")
    @PreAuthorize("hasAnyRole('DESK','POLICE','ADMIN')")
    public Page<FoundReportResponse> listFound(@RequestParam(required = false) ReportStatus status,
                                               @PageableDefault(size = 20, sort = "foundAt") Pageable pageable) {
        return reports.listFound(status, Pages.capped(pageable), CurrentUser.get());
    }

    @GetMapping("/missing/{id}/matches")
    @PreAuthorize("hasAnyRole('DESK','POLICE')")
    @Operation(summary = "Ranked suggestions (score >= 50) with factor breakdown. Suggestions only, never auto-confirmed.")
    public List<MatchSuggestion> matches(@PathVariable Long id) {
        return matching.suggestionsFor(id, CurrentUser.get());
    }
}
