package com.karimsammouri.resolve.controller;

import com.karimsammouri.resolve.config.SecurityConfig;
import com.karimsammouri.resolve.config.SpringDataWebConfig;
import com.karimsammouri.resolve.dto.request.CreateUserRequest;
import com.karimsammouri.resolve.dto.request.UpdateUserRequest;
import com.karimsammouri.resolve.dto.response.ViewUserResponse;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@EnableWebSecurity
@WebMvcTest(AdminController.class)
@Import({SecurityConfig.class, SpringDataWebConfig.class})
public class AdminControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdminService adminService;

    private Long adminId;
    private Long organizationId;
    private ResolveUserDetails principal;

    @BeforeEach
    void setup() {
        adminId = 1L;
        organizationId = 10L;
        principal = mock(ResolveUserDetails.class);

        when(principal.getId()).thenReturn(adminId);
        when(principal.getOrganizationId()).thenReturn(organizationId);
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR"))).when(principal).getAuthorities();
    }

    @Test
    void createUser_withValidRequest_shouldReturnCreated() throws Exception {
        CreateUserRequest request = new CreateUserRequest(
                "John Doe",
                "john@example.com",
                "password123",
                UserRole.AGENT);

        ViewUserResponse response = new ViewUserResponse(
                456L,
                "John Doe",
                "john@example.com",
                UserRole.AGENT,
                true,
                LocalDateTime.now());

        when(adminService.createUser(organizationId, request)).thenReturn(response);

        mockMvc.perform(post("/api/admin/users")
                        .with(user(principal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.userId").value(456))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.role").value("AGENT"))
                .andExpect(jsonPath("$.isEnabled").value(true));

        verify(adminService).createUser(organizationId, request);
    }

    @Test
    void createUser_withDuplicateEmail_shouldReturnConflict() throws Exception {
        CreateUserRequest request = new CreateUserRequest(
                "John Doe",
                "john@example.com",
                "password123",
                UserRole.AGENT);

        when(adminService.createUser(organizationId, request))
                .thenThrow(new DuplicateResourceException("Email already exists"));

        mockMvc.perform(post("/api/admin/users")
                        .with(user(principal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Email already exists"));

        verify(adminService).createUser(organizationId, request);
    }

    @Test
    void viewUsers_withPagination_shouldReturnPage() throws Exception {
        Pageable pageable = PageRequest.of(0, 5);
        Page<ViewUserResponse> page = new PageImpl<>(List.of(), pageable, 0);

        when(adminService.viewUsers(organizationId, pageable)).thenReturn(page);

        mockMvc.perform(get("/api/admin/users")
                        .with(user(principal))
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$.page.size").value(5))
                .andExpect(jsonPath("$.page.totalElements").value(0))
                .andExpect(jsonPath("$.content").isEmpty());

        verify(adminService).viewUsers(organizationId, pageable);
    }

    @Test
    void updateUser_withValidRequest_shouldReturnNoContent() throws Exception {
        UpdateUserRequest request = new UpdateUserRequest(
                "Jane Doe",
                null,
                null,
                null,
                null);

        mockMvc.perform(patch("/api/admin/users/123")
                        .with(user(principal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(adminService).updateUser(123L, adminId, organizationId, request);
    }

    @Test
    void updateUser_withNonexistentUser_shouldReturnNotFound() throws Exception {
        UpdateUserRequest request = new UpdateUserRequest(
                "Jane Doe",
                null,
                null,
                null,
                null);

        doThrow(new ResourceNotFoundException("User not found"))
                .when(adminService).updateUser(999L, adminId, organizationId, request);

        mockMvc.perform(patch("/api/admin/users/999")
                        .with(user(principal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("User not found"));

        verify(adminService).updateUser(999L, adminId, organizationId, request);
    }
}
