package com.rajesh.milansetu.controller;

import com.rajesh.milansetu.dto.request.CreateUserRequest;
import com.rajesh.milansetu.dto.request.UpdateUserRequest;
import com.rajesh.milansetu.dto.response.UserResponse;
import com.rajesh.milansetu.entity.Role;
import com.rajesh.milansetu.security.CurrentUser;
import com.rajesh.milansetu.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users (admin)")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {
    private final UserService users;

    public UserController(UserService users) {
        this.users = users;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody CreateUserRequest req) {
        return users.create(req, CurrentUser.get());
    }

    @GetMapping
    public Page<UserResponse> list(@RequestParam(required = false) Role role,
                                   @PageableDefault(size = 20, sort = "fullName") Pageable pageable) {
        return users.list(role, Pages.capped(pageable));
    }

    @PatchMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest req) {
        return users.update(id, req, CurrentUser.get());
    }
}
