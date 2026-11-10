package com.rajesh.milansetu.controller;

import com.rajesh.milansetu.dto.response.DashboardSummary;
import com.rajesh.milansetu.security.CurrentUser;
import com.rajesh.milansetu.service.DashboardService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard")
public class DashboardController {
    private final DashboardService dashboard;

    public DashboardController(DashboardService dashboard) {
        this.dashboard = dashboard;
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('POLICE','ADMIN')")
    public DashboardSummary summary(@RequestParam(required = false) Long eventId,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return dashboard.summary(eventId, from, to, CurrentUser.get());
    }
}
