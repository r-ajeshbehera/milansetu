package com.rajesh.milansetu.service;

import com.rajesh.milansetu.dto.request.LoginRequest;
import com.rajesh.milansetu.dto.request.RegisterRequest;
import com.rajesh.milansetu.dto.response.LoginResponse;
import com.rajesh.milansetu.dto.response.ProfileResponse;
import com.rajesh.milansetu.entity.RevokedToken;
import com.rajesh.milansetu.entity.Role;
import com.rajesh.milansetu.entity.User;
import com.rajesh.milansetu.exception.BusinessRuleException;
import com.rajesh.milansetu.repository.RevokedTokenRepository;
import com.rajesh.milansetu.repository.UserRepository;
import com.rajesh.milansetu.security.AppUserPrincipal;
import com.rajesh.milansetu.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwt;
    private final UserRepository users;
    private final RevokedTokenRepository revoked;
    private final PasswordEncoder encoder;
    private final AuditService audit;

    public AuthService(AuthenticationManager authenticationManager, JwtService jwt, UserRepository users,
                       RevokedTokenRepository revoked, PasswordEncoder encoder, AuditService audit) {
        this.authenticationManager = authenticationManager;
        this.jwt = jwt;
        this.users = users;
        this.revoked = revoked;
        this.encoder = encoder;
        this.audit = audit;
    }

    /** Public registration ALWAYS creates a FAMILY account. The request has no role field at all. */
    @Transactional
    public ProfileResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            throw new BusinessRuleException("EMAIL_TAKEN", "An account with this email already exists");
        }
        User u = users.save(new User(req.fullName().trim(), email, req.phone(), encoder.encode(req.password()), Role.FAMILY));
        audit.log(u.getId(), "REGISTERED", "USER", u.getId(), null);
        return ProfileResponse.from(u);
    }

    @Transactional
    public LoginResponse login(LoginRequest req) {
        var auth = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(req.email().trim(), req.password()));
        AppUserPrincipal p = (AppUserPrincipal) auth.getPrincipal();
        JwtService.IssuedToken token = jwt.issue(p);
        audit.log(p.id(), "LOGIN", "USER", p.id(), null);
        return new LoginResponse(token.token(), "Bearer", token.expiresAt(), ProfileResponse.from(users.findById(p.id()).orElseThrow()));
    }

    @Transactional
    public void logout(String jti, Instant expiresAt, Long userId) {
        if (jti == null || revoked.existsById(jti)) return;
        revoked.save(new RevokedToken(jti, LocalDateTime.ofInstant(expiresAt, ZoneOffset.UTC)));
        audit.log(userId, "LOGOUT", "USER", userId, null);
    }
}
