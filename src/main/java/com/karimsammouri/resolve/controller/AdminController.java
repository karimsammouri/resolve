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
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<Long> createUser(@AuthenticationPrincipal ResolveUserDetails principal,
                                           @Valid @RequestBody CreateUserRequest request) {
        Long newUserId = adminService.createUser(principal.getUser(), request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(newUserId);
    }

    @GetMapping("/users")
    public ResponseEntity<Page<ViewUserResponse>> viewUsers(@AuthenticationPrincipal ResolveUserDetails principal,
                                                            Pageable pageable) {
        Page<ViewUserResponse> response = adminService.viewUsers(principal.getUser(), pageable);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/users/{userId}")
    public ResponseEntity<Long> updateUser(@PathVariable Long userId,
                                           @AuthenticationPrincipal ResolveUserDetails principal,
                                           @RequestBody UpdateUserRequest request) {
        Long updatedUserId = adminService.updateUser(userId, principal.getUser(), request);

        return ResponseEntity.ok(updatedUserId);
    }
}
