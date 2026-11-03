package com.karimsammouri.resolve.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TokenServiceTest {
    private final TokenService tokenService = new TokenService();

    @Test
    void generateToken_shouldReturn43CharacterUrlSafeToken() {
        String token = tokenService.generateToken();

        assertEquals(43, token.length());
        assertTrue(token.matches("[A-Za-z0-9_-]+"));
    }

    @Test
    void hashToken_shouldReturnExpectedSha256Hash() {
        String token = "hello";
        String expectedHash = "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824";

        String generatedHash = tokenService.hashToken(token);

        assertEquals(expectedHash, generatedHash);
    }
}
