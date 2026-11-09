package com.karimsammouri.resolve.controller;

import com.karimsammouri.resolve.config.SecurityConfig;
import com.karimsammouri.resolve.dto.request.CreateUserRequest;
import com.karimsammouri.resolve.dto.request.UpdateUserRequest;
import com.karimsammouri.resolve.dto.response.ViewUserResponse;
import com.karimsammouri.resolve.entity.Organization;
import com.karimsammouri.resolve.entity.User;
import com.karimsammouri.resolve.enums.UserRole;
import com.karimsammouri.resolve.exception.DuplicateResourceException;
import com.karimsammouri.resolve.exception.ResourceNotFoundException;
import com.karimsammouri.resolve.security.ResolveUserDetails;
import com.karimsammouri.resolve.service.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@EnableWebSecurity
@WebMvcTest(AdminController.class)
@Import(SecurityConfig.class)
public class AdminControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdminService adminService;

    private User user;
    private ResolveUserDetails principal;

    @BeforeEach
    void setup() {
        user = new User(
                "Admin Smith",
                "admin@acme.com",
                "hashed-password",
                UserRole.ADMINISTRATOR,
                mock(Organization.class));

        principal = new ResolveUserDetails(user);
    }

    @Test
    void createUser_withValidRequest_shouldReturnCreated() throws Exception {
        CreateUserRequest request = new CreateUserRequest(
                "John Doe",
                "john@example.com",
                "password123",
                UserRole.AGENT
        );

        when(adminService.createUser(user, request)).thenReturn(456L);

        mockMvc.perform(post("/api/admin/users")
                        .with(user(principal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().string("456"));

        verify(adminService).createUser(user, request);
    }

    @Test
    void createUser_withDuplicateEmail_shouldReturnConflict() throws Exception {
        CreateUserRequest request = new CreateUserRequest(
                "John Doe",
                "john@example.com",
                "password123",
                UserRole.AGENT
        );

        when(adminService.createUser(user, request)).thenThrow(new DuplicateResourceException("Email already exists"));

        mockMvc.perform(post("/api/admin/users")
                        .with(user(principal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Email already exists"));

        verify(adminService).createUser(user, request);
    }

    @Test
    void viewUsers_withPagination_shouldReturnPage() throws Exception {
        Pageable pageable = PageRequest.of(0, 5);
        Page<ViewUserResponse> page = new PageImpl<>(List.of(), pageable, 0);

        when(adminService.viewUsers(user, pageable)).thenReturn(page);

        mockMvc.perform(get("/api/admin/users")
                        .with(user(principal))
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.content").isEmpty());

        verify(adminService).viewUsers(user, pageable);
    }

    @Test
    void updateUser_withValidRequest_shouldReturnOk() throws Exception {
        UpdateUserRequest request = new UpdateUserRequest(
                "Jane Doe",
                null,
                null,
                null,
                null
        );

        when(adminService.updateUser(123L, user, request))
                .thenReturn(123L);

        mockMvc.perform(patch("/api/admin/users/123")
                        .with(user(principal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().string("123"));

        verify(adminService).updateUser(123L, user, request);
    }

    @Test
    void updateUser_withNonexistentUser_shouldReturnNotFound() throws Exception {
        UpdateUserRequest request = new UpdateUserRequest(
                "Jane Doe",
                null,
                null,
                null,
                null
        );

        when(adminService.updateUser(999L, user, request))
                .thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(patch("/api/admin/users/999")
                        .with(user(principal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("User not found"));

        verify(adminService).updateUser(999L, user, request);
    }
}
