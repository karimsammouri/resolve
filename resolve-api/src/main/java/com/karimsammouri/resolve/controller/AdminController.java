package com.karimsammouri.resolve.controller;

import com.karimsammouri.resolve.dto.request.CreateUserRequest;
import com.karimsammouri.resolve.dto.request.UpdateUserRequest;
import com.karimsammouri.resolve.dto.response.ViewUserResponse;
import com.karimsammouri.resolve.security.ResolveUserDetails;
import com.karimsammouri.resolve.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public Long createUser(@AuthenticationPrincipal ResolveUserDetails principal,
                           @Valid @RequestBody CreateUserRequest request) {
        return adminService.createUser(principal.getUser(), request);
    }

    @GetMapping("/users")
    public Page<ViewUserResponse> viewUsers(@AuthenticationPrincipal ResolveUserDetails principal, Pageable pageable) {
        return adminService.viewUsers(principal.getUser(), pageable);
    }

    @PatchMapping("/users/{userId}")
    public Long updateUser(@PathVariable Long userId,
                           @AuthenticationPrincipal ResolveUserDetails principal,
                           @RequestBody UpdateUserRequest request) {
        return adminService.updateUser(userId, principal.getUser(), request);
    }
}
