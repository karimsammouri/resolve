package com.karimsammouri.resolve.service;

import com.karimsammouri.resolve.dto.request.CreateUserRequest;
import com.karimsammouri.resolve.dto.request.UpdateUserRequest;
import com.karimsammouri.resolve.dto.response.ViewUserResponse;
import com.karimsammouri.resolve.entity.Organization;
import com.karimsammouri.resolve.entity.User;
import com.karimsammouri.resolve.enums.UserRole;
import com.karimsammouri.resolve.exception.DuplicateResourceException;
import com.karimsammouri.resolve.exception.ResourceNotFoundException;
import com.karimsammouri.resolve.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AdminServiceTest {
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

    private final UserRepository userRepo = mock(UserRepository.class);

    private final AdminService adminService = new AdminService(passwordEncoder, userRepo);

    private Long organizationId;
    private Organization organization;
    private Long adminId;
    private User admin;

    @BeforeEach
    void setup() {
        organizationId = 1L;
        organization = new Organization("Acme", "acme");
        ReflectionTestUtils.setField(organization, "id", organizationId);

        adminId = 1L;
        admin = new User(
                "Admin Smith",
                "admin@acme.com",
                "hashed-password",
                UserRole.ADMINISTRATOR,
                organization);
        ReflectionTestUtils.setField(admin, "id", adminId);
    }

    @Test
    void createUser_shouldSaveNewUser() {
        CreateUserRequest request = new CreateUserRequest(
                "Agent Smith",
                "agent@acme.com",
                "password123",
                UserRole.AGENT);

        when(userRepo.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");

        adminService.createUser(admin, request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepo).save(captor.capture());

        User newUser = captor.getValue();
        assertEquals("Agent Smith", newUser.getName());
        assertEquals("agent@acme.com", newUser.getEmail());
        assertEquals("encoded-password", newUser.getPasswordHash());
        assertEquals(UserRole.AGENT, newUser.getRole());
        assertEquals(admin.getOrganization(), newUser.getOrganization());
        assertTrue(newUser.isEnabled());
    }

    @Test
    void createUser_shouldRejectTakenEmail() {
        CreateUserRequest request = new CreateUserRequest(
                "Agent Smith",
                "agent@acme.com",
                "password123",
                UserRole.AGENT);

        when(userRepo.existsByEmail(request.email())).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> adminService.createUser(admin, request));

        verify(userRepo, never()).save(any(User.class));
    }

    @Test
    void viewUsers_shouldReturnUsersInAdminOrganization() {
        User user1 = new User(
                "Agent Smith",
                "agentsmith@acme.com",
                "hashed-password",
                UserRole.AGENT,
                organization);

        User user2 = new User(
                "Agent Doe",
                "agentdoe@acme.com",
                "hashed-password",
                UserRole.ADMINISTRATOR,
                organization);
        user2.setEnabled(false);

        Pageable pageable = PageRequest.of(0, 2);
        Page<User> userPage = new PageImpl<>(List.of(user1, user2), pageable, 5);

        when(userRepo.findByOrganization_Id(organizationId, pageable))
                .thenReturn(userPage);

        Page<ViewUserResponse> result = adminService.viewUsers(admin, pageable);
        assertEquals(2, result.getContent().size());
        assertEquals(0, result.getNumber());
        assertEquals(2, result.getSize());
        assertEquals(5, result.getTotalElements());
        assertEquals(3, result.getTotalPages());

        ViewUserResponse firstUser = result.getContent().get(0);
        assertEquals("Agent Smith", firstUser.name());
        assertEquals("agentsmith@acme.com", firstUser.email());
        assertEquals(UserRole.AGENT, firstUser.role());
        assertTrue(firstUser.isEnabled());
        assertNotNull(firstUser.createdAt());

        ViewUserResponse secondUser = result.getContent().get(1);
        assertEquals("Agent Doe", secondUser.name());
        assertEquals("agentdoe@acme.com", secondUser.email());
        assertEquals(UserRole.ADMINISTRATOR, secondUser.role());
        assertFalse(secondUser.isEnabled());
        assertNotNull(secondUser.createdAt());
    }

    @Test
    void viewUsers_shouldReturnEmptyPageWhenNoUsersExist() {
        Pageable pageable = PageRequest.of(0, 2);
        when(userRepo.findByOrganization_Id(organizationId, pageable))
                .thenReturn(Page.empty(pageable));

        Page<ViewUserResponse> result = adminService.viewUsers(admin, pageable);
        assertTrue(result.isEmpty());
    }

    @Test
    void updateUser_shouldUpdateUserProperties() {
        Long targetUserId = 2L;

        User user = new User(
                "Agent Smith",
                "agent@acme.com",
                "hashed-password",
                UserRole.AGENT,
                organization);

        when(userRepo.findByIdAndOrganization_Id(targetUserId, organizationId))
                .thenReturn(Optional.of(user));
        when(userRepo.existsByEmail("adminsmith@acme.com"))
                .thenReturn(false);

        UpdateUserRequest request = new UpdateUserRequest(
                "Admin Smith",
                "admin@acme.com",
                null,
                UserRole.ADMINISTRATOR,
                null);

        adminService.updateUser(targetUserId, admin, request);

        assertEquals("Admin Smith", user.getName());
        assertEquals("admin@acme.com", user.getEmail());
        assertEquals("hashed-password", user.getPasswordHash());
        assertEquals(UserRole.ADMINISTRATOR, user.getRole());
        assertEquals(admin.getOrganization(), user.getOrganization());
        assertTrue(user.isEnabled());
    }

    @Test
    void updateUser_shouldRejectInvalidUser() {
        Long targetUserId = 5L;

        when(userRepo.findByIdAndOrganization_Id(targetUserId, organizationId))
                .thenReturn(Optional.empty());

        UpdateUserRequest request = new UpdateUserRequest(
                "Agent Smith",
                "agent@acme.com",
                "password-123",
                UserRole.ADMINISTRATOR,
                true);

        assertThrows(ResourceNotFoundException.class, () -> adminService.updateUser(targetUserId, admin, request));
    }

    @Test
    void updateUser_shouldRejectTakenEmail() {
        Long targetUserId = 2L;

        User user = new User(
                "Agent Smith",
                "agent@acme.com",
                "hashed-password",
                UserRole.AGENT,
                organization);

        when(userRepo.findByIdAndOrganization_Id(targetUserId, organizationId))
                .thenReturn(Optional.of(user));
        when(userRepo.existsByEmail("admin@acme.com"))
                .thenReturn(true);

        UpdateUserRequest request = new UpdateUserRequest(
                "Admin Smith",
                "admin@acme.com",
                null,
                UserRole.ADMINISTRATOR,
                null);

        assertThrows(DuplicateResourceException.class, () -> adminService.updateUser(targetUserId, admin, request));
        assertEquals("agent@acme.com", user.getEmail());
    }

    @Test
    void updateUser_shouldPreventAdminFromChangingOwnRoleAndEnabledStatus() {
        when(userRepo.findByIdAndOrganization_Id(adminId, organizationId))
                .thenReturn(Optional.of(admin));

        UpdateUserRequest request = new UpdateUserRequest(
                null,
                null,
                null,
                UserRole.AGENT,
                false);

        adminService.updateUser(adminId, admin, request);

        assertEquals("Admin Smith", admin.getName());
        assertEquals("admin@acme.com", admin.getEmail());
        assertEquals("hashed-password", admin.getPasswordHash());
        assertEquals(UserRole.ADMINISTRATOR, admin.getRole());
        assertTrue(admin.isEnabled());
    }
}

