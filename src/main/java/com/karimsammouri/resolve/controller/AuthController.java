package com.karimsammouri.resolve.controller;

import com.karimsammouri.resolve.dto.request.LoginRequest;
import com.karimsammouri.resolve.dto.response.AuthResponse;
import com.karimsammouri.resolve.entity.User;
import com.karimsammouri.resolve.security.ResolveUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;

    private final SecurityContextRepository securityContextRepo;
    private final CsrfTokenRepository csrfTokenRepo;

    public AuthController(AuthenticationManager authenticationManager,
                          SecurityContextRepository securityContextRepo,
                          CsrfTokenRepository csrfTokenRepo) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepo = securityContextRepo;
        this.csrfTokenRepo = csrfTokenRepo;
    }

    @GetMapping("/csrf")
    public CsrfToken csrf(CsrfToken csrfToken) {
        return csrfToken;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletRequest httpRequest,
                                              HttpServletResponse httpResponse) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            request.email(),
                            request.password()));

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            // Protect against session fixation
            if (httpRequest.getSession(false) != null) {
                httpRequest.changeSessionId();
            }

            // Persist authentication in the HTTP session
            securityContextRepo.saveContext(context, httpRequest, httpResponse);

            csrfTokenRepo.saveToken(null, httpRequest, httpResponse);

            ResolveUserDetails userDetails = (ResolveUserDetails) authentication.getPrincipal();

            User user = userDetails.getUser();

            AuthResponse response = new AuthResponse(
                    user.getName(),
                    user.getEmail(),
                    user.getRole());

            return ResponseEntity.ok(response);

        } catch (AuthenticationException exception) {
            SecurityContextHolder.clearContext();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> me(Authentication authentication) {
        ResolveUserDetails userDetails = (ResolveUserDetails) authentication.getPrincipal();

        User user = userDetails.getUser();

        AuthResponse response = new AuthResponse(
                user.getName(),
                user.getEmail(),
                user.getRole());

        return ResponseEntity.ok(response);
    }
}
