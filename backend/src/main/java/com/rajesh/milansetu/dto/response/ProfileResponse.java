package com.rajesh.milansetu.dto.response;

import com.rajesh.milansetu.entity.Role;
import com.rajesh.milansetu.entity.User;

/** The caller's OWN profile: the only DTO that contains a phone number. */
public record ProfileResponse(Long id, String fullName, String email, String phone, Role role) {
    public static ProfileResponse from(User u) {
        return new ProfileResponse(u.getId(), u.getFullName(), u.getEmail(), u.getPhone(), u.getRole());
    }
}
