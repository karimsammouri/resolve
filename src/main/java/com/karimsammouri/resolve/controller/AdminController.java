package com.karimsammouri.resolve.controller;

import com.karimsammouri.resolve.dto.CreateUserRequest;
import com.karimsammouri.resolve.dto.ViewUserResponse;
import com.karimsammouri.resolve.entity.User;
import com.karimsammouri.resolve.enums.UserRole;
import com.karimsammouri.resolve.repository.UserRepository;
import com.karimsammouri.resolve.service.AdminService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService adminService;

    private final UserRepository userRepo;

    public AdminController(AdminService adminService,
                           UserRepository userRepo) {
        this.adminService = adminService;
        this.userRepo = userRepo;
    }

    @PostMapping("/users")
    public ResponseEntity<Long> createUser(@RequestParam Long userId,
                                           @Valid @RequestBody CreateUserRequest request) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (!user.isEnabled() || user.getRole() != UserRole.ADMINISTRATOR) {
            throw new SecurityException("This operation cannot be performed");
        }

        Long newUserId = adminService.createUser(user, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(newUserId);
    }

    @GetMapping("/users")
    public ResponseEntity<Page<ViewUserResponse>> viewUsers(@RequestParam Long userId,
                                                            Pageable pageable) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (!user.isEnabled() || user.getRole() != UserRole.ADMINISTRATOR) {
            throw new SecurityException("This operation cannot be performed");
        }

        Page<ViewUserResponse> response = adminService.viewUsers(user, pageable);

        return ResponseEntity.ok(response);
    }
}
