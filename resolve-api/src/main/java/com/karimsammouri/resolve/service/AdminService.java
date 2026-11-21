package com.karimsammouri.resolve.service;

import com.karimsammouri.resolve.dto.request.CreateUserRequest;
import com.karimsammouri.resolve.dto.request.UpdateUserRequest;
import com.karimsammouri.resolve.dto.response.ViewUserResponse;
import com.karimsammouri.resolve.entity.User;
import com.karimsammouri.resolve.exception.DuplicateResourceException;
import com.karimsammouri.resolve.exception.ForbiddenOperationException;
import com.karimsammouri.resolve.exception.ResourceNotFoundException;
import com.karimsammouri.resolve.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {
    private final PasswordEncoder passwordEncoder;

    private final UserRepository userRepo;

    public AdminService(PasswordEncoder passwordEncoder, UserRepository userRepo) {
        this.passwordEncoder = passwordEncoder;
        this.userRepo = userRepo;
    }

    @Transactional
    public ViewUserResponse createUser(User admin, CreateUserRequest request) {
        // Confirm that email is not already in use
        if (userRepo.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already exists");
        }

        // Create and save new user
        User newUser = new User(
                request.name(),
                request.email(),
                passwordEncoder.encode(request.password()),
                request.role(),
                admin.getOrganization());
        userRepo.save(newUser);

        return new ViewUserResponse(
                newUser.getId(),
                newUser.getName(),
                newUser.getEmail(),
                newUser.getRole(),
                newUser.isEnabled(),
                newUser.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public Page<ViewUserResponse> viewUsers(User admin, Pageable pageable) {
        // Retrieve and return users scoped to admin's organization
        Page<User> users = userRepo.findByOrganization_Id(admin.getOrganization().getId(), pageable);

        return users.map(user -> new ViewUserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.isEnabled(),
                user.getCreatedAt()));
    }

    @Transactional
    public void updateUser(Long userId, User admin, UpdateUserRequest request) {
        // Retrieve user scoped by organization
        User user = userRepo.findByIdAndOrganization_Id(userId, admin.getOrganization().getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: #" + userId));

        // Prevent administrator from changing their own role or enabled status
        if (userId.equals(admin.getId()) &&
                (request.role() != null || request.isEnabled() != null)) {
            throw new ForbiddenOperationException("Administrators cannot change their own role or enabled status");
        }

        // Validate email before modifying entity
        if (request.email() != null &&
                !request.email().equals(user.getEmail()) &&
                userRepo.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already exists");
        }

        // Update user properties
        if (request.name() != null) {
            user.setName(request.name());
        }
        if (request.email() != null) {
            user.setEmail(request.email());
        }
        if (request.password() != null) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        if (request.role() != null) {
            user.setRole(request.role());
        }
        if (request.isEnabled() != null) {
            user.setEnabled(request.isEnabled());
        }
    }
}
