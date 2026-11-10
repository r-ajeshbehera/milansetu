package com.rajesh.milansetu.dto.response;

import com.rajesh.milansetu.entity.Role;
import com.rajesh.milansetu.entity.User;

/** Used for admin lists. Deliberately has NO phone field. */
public record UserResponse(Long id, String fullName, String email, Role role, boolean active) {
    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getFullName(), u.getEmail(), u.getRole(), u.isActive());
    }
}
