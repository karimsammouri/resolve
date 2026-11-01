package com.karimsammouri.resolve.service;

import com.karimsammouri.resolve.dto.CreateUserRequest;
import com.karimsammouri.resolve.entity.User;
import com.karimsammouri.resolve.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {
    private final UserRepository userRepo;

    public AdminService(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @Transactional
    public Long createUser(User admin, CreateUserRequest request) {
        // Confirm that email is not already in use
        if (userRepo.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Create and save new user
        User newUser = new User(
                request.name(),
                request.email(),
                // Use passwordEncoder.encode(request.password()) once Spring Security is enabled
                request.password(),
                request.role(),
                admin.getOrganization());
        userRepo.save(newUser);

        return newUser.getId();
    }
}
