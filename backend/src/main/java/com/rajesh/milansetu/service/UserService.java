package com.rajesh.milansetu.service;

import com.rajesh.milansetu.dto.request.CreateUserRequest;
import com.rajesh.milansetu.dto.request.UpdateProfileRequest;
import com.rajesh.milansetu.dto.request.UpdateUserRequest;
import com.rajesh.milansetu.dto.response.ProfileResponse;
import com.rajesh.milansetu.dto.response.UserResponse;
import com.rajesh.milansetu.entity.Role;
import com.rajesh.milansetu.entity.User;
import com.rajesh.milansetu.exception.BusinessRuleException;
import com.rajesh.milansetu.exception.ResourceNotFoundException;
import com.rajesh.milansetu.repository.UserRepository;
import com.rajesh.milansetu.security.AppUserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Staff accounts (VOLUNTEER, DESK, POLICE, ADMIN) are created only by an ADMIN. */
@Service
public class UserService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AuditService audit;

    public UserService(UserRepository users, PasswordEncoder encoder, AuditService audit) {
        this.users = users;
        this.encoder = encoder;
        this.audit = audit;
    }

    @Transactional
    public UserResponse create(CreateUserRequest req, AppUserPrincipal admin) {
        if (req.role() == Role.FAMILY) {
            throw new BusinessRuleException("USE_REGISTRATION", "Family accounts are created through public registration");
        }
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) throw new BusinessRuleException("EMAIL_TAKEN", "Email already in use");
        User u = users.save(new User(req.fullName().trim(), email, req.phone(), encoder.encode(req.password()), req.role()));
        audit.log(admin.id(), "USER_CREATED", "USER", u.getId(), "role=" + u.getRole());
        return UserResponse.from(u);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> list(Role role, Pageable pageable) {
        return users.search(role, pageable).map(UserResponse::from);
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest req, AppUserPrincipal admin) {
        User u = users.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
        if (req.role() != null && (req.role() == Role.FAMILY) != (u.getRole() == Role.FAMILY)) {
            throw new BusinessRuleException("ROLE_CHANGE_NOT_ALLOWED", "Cannot convert between family and staff accounts");
        }
        if (Boolean.FALSE.equals(req.active()) && id.equals(admin.id())) {
            throw new BusinessRuleException("SELF_DEACTIVATION", "You cannot deactivate your own account");
        }
        if (req.fullName() != null) u.setFullName(req.fullName().trim());
        if (req.active() != null) u.setActive(req.active());
        if (req.role() != null) u.setRole(req.role());
        audit.log(admin.id(), "USER_UPDATED", "USER", id, null);
        return UserResponse.from(u);
    }

    @Transactional(readOnly = true)
    public ProfileResponse me(AppUserPrincipal actor) {
        return ProfileResponse.from(users.findById(actor.id()).orElseThrow(() -> new ResourceNotFoundException("User", actor.id())));
    }

    @Transactional
    public ProfileResponse updateProfile(UpdateProfileRequest req, AppUserPrincipal actor) {
        User u = users.findById(actor.id()).orElseThrow(() -> new ResourceNotFoundException("User", actor.id()));
        if (req.fullName() != null) u.setFullName(req.fullName().trim());
        if (req.phone() != null) u.setPhone(req.phone());
        return ProfileResponse.from(u);
    }
}
