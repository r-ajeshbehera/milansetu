package com.rajesh.milansetu.controller;

import com.rajesh.milansetu.dto.request.ScanRequest;
import com.rajesh.milansetu.dto.response.ScanResponse;
import com.rajesh.milansetu.security.CurrentUser;
import com.rajesh.milansetu.service.ScanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/scan")
@Tag(name = "QR scan")
public class ScanController {
    private final ScanService scans;

    public ScanController(ScanService scans) {
        this.scans = scans;
    }

    @PostMapping("/{qrToken}")
    @PreAuthorize("hasAnyRole('VOLUNTEER','DESK')")
    @Operation(summary = "Scan a wristband: 201 new found case, 200 existing open case. Never returns guardian details.")
    public ResponseEntity<ScanResponse> scan(@PathVariable String qrToken, @Valid @RequestBody(required = false) ScanRequest req) {
        ScanResponse res = scans.scan(qrToken, req, CurrentUser.get());
        return ResponseEntity.status(res.newCase() ? HttpStatus.CREATED : HttpStatus.OK).body(res);
    }
}
