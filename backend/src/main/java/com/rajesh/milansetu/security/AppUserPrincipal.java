package com.rajesh.milansetu.security;

import com.rajesh.milansetu.entity.Role;
import com.rajesh.milansetu.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public record AppUserPrincipal(Long id, String email, String passwordHash, Role role, boolean active) implements UserDetails {

    public static AppUserPrincipal from(User u) {
        return new AppUserPrincipal(u.getId(), u.getEmail(), u.getPasswordHash(), u.getRole(), u.isActive());
    }

    public boolean isAdmin() { return role == Role.ADMIN; }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return email; }
    @Override public boolean isEnabled() { return active; }
}
