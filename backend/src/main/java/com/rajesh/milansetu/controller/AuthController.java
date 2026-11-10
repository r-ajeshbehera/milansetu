package com.rajesh.milansetu.controller;

import com.rajesh.milansetu.dto.request.LoginRequest;
import com.rajesh.milansetu.dto.request.RegisterRequest;
import com.rajesh.milansetu.dto.request.UpdateProfileRequest;
import com.rajesh.milansetu.dto.response.LoginResponse;
import com.rajesh.milansetu.dto.response.ProfileResponse;
import com.rajesh.milansetu.security.CurrentUser;
import com.rajesh.milansetu.security.JwtAuthenticationFilter;
import com.rajesh.milansetu.service.AuthService;
import com.rajesh.milansetu.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api")
@Tag(name = "Authentication and profile")
public class AuthController {
    private final AuthService auth;
    private final UserService users;

    public AuthController(AuthService auth, UserService users) {
        this.auth = auth;
        this.users = users;
    }

    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Public family registration (always role FAMILY)")
    public ProfileResponse register(@Valid @RequestBody RegisterRequest req) {
        return auth.register(req);
    }

    @PostMapping("/auth/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        return auth.login(req);
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        auth.logout((String) request.getAttribute(JwtAuthenticationFilter.JTI_ATTRIBUTE),
                (Instant) request.getAttribute(JwtAuthenticationFilter.EXP_ATTRIBUTE), CurrentUser.get().id());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users/me")
    public ProfileResponse me() {
        return users.me(CurrentUser.get());
    }

    @PatchMapping("/users/me")
    public ProfileResponse updateMe(@Valid @RequestBody UpdateProfileRequest req) {
        return users.updateProfile(req, CurrentUser.get());
    }
}
