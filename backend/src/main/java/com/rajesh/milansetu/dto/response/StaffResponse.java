package com.rajesh.milansetu.dto.response;

import com.rajesh.milansetu.entity.Role;

public record StaffResponse(Long userId, String fullName, Role role, Long deskId) { }
