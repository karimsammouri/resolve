package com.karimsammouri.resolve.controller;

import com.karimsammouri.resolve.config.SecurityConfig;
import com.karimsammouri.resolve.dto.request.LoginRequest;
import com.karimsammouri.resolve.enums.UserRole;
import com.karimsammouri.resolve.security.ResolveUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@EnableWebSecurity
@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    private ResolveUserDetails principal;

    @BeforeEach
    void setup() {
        principal = mock(ResolveUserDetails.class);

        when(principal.getName()).thenReturn("Agent Smith");
        when(principal.getUsername()).thenReturn("agent@acme.com");
        when(principal.getRole()).thenReturn(UserRole.AGENT);
    }

    @Test
    void login_withValidCredentials_shouldReturnOk() throws Exception {
        LoginRequest request = new LoginRequest(
                "agent@acme.com",
                "password123");

        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_AGENT")));

        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Agent Smith"))
                .andExpect(jsonPath("$.email").value("agent@acme.com"))
                .andExpect(jsonPath("$.role").value("AGENT"));

        verify(authenticationManager).authenticate(any(Authentication.class));
    }

    @Test
    void login_withInvalidCredentials_shouldReturnUnauthorized() throws Exception {
        LoginRequest request = new LoginRequest("agent@acme.com", "wrong-password"
        );

        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_withAuthentication_shouldReturnCurrentUser() throws Exception {
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_AGENT"))).when(principal).getAuthorities();

        mockMvc.perform(get("/api/auth/me")
                        .with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Agent Smith"))
                .andExpect(jsonPath("$.email").value("agent@acme.com"))
                .andExpect(jsonPath("$.role").value("AGENT"));
    }

    @Test
    void me_withoutAuthentication_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }
}