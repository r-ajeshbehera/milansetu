package com.rajesh.milansetu.controller;

import com.rajesh.milansetu.dto.request.MemberRequest;
import com.rajesh.milansetu.dto.response.MemberResponse;
import com.rajesh.milansetu.security.CurrentUser;
import com.rajesh.milansetu.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/members")
@Tag(name = "Family members and QR wristbands")
@PreAuthorize("hasRole('FAMILY')")
public class MemberController {
    private final MemberService members;

    public MemberController(MemberService members) {
        this.members = members;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponse create(@Valid @RequestBody MemberRequest req) {
        return members.create(req, CurrentUser.get());
    }

    @GetMapping
    public List<MemberResponse> mine() {
        return members.mine(CurrentUser.get());
    }

    @GetMapping("/{id}")
    public MemberResponse get(@PathVariable Long id) {
        return members.get(id, CurrentUser.get());
    }

    @GetMapping(value = "/{id}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    @Operation(summary = "QR code PNG (owner only). Payload contains only a random token.")
    public ResponseEntity<byte[]> qr(@PathVariable Long id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.IMAGE_PNG)
                .body(members.qrPng(id, CurrentUser.get()));
    }

    @GetMapping(value = "/{id}/wristband", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> wristband(@PathVariable Long id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.TEXT_HTML)
                .body(members.wristbandHtml(id, CurrentUser.get()));
    }

    @PostMapping("/{id}/qr/regenerate")
    public MemberResponse regenerate(@PathVariable Long id) {
        return members.regenerate(id, CurrentUser.get());
    }
}
