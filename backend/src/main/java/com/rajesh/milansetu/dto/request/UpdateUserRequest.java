package com.rajesh.milansetu.dto.request;

import com.rajesh.milansetu.entity.Role;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(@Size(max = 120) String fullName, Boolean active, Role role) { }
