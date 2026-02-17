package com.spotify.dashboard.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * unit tests for TokenUtil
 * 
 * tests the token extraction logic without any Spring context or dependencies.
 * this is a pure unit test - fast, isolated, and easy to understand.
 */
@DisplayName("TokenUtil Unit Tests")
class TokenUtilTest {

    private TokenUtil tokenUtil;

    @BeforeEach
    void setUp() {
        tokenUtil = new TokenUtil();
    }

    @Test
    @DisplayName("Should extract token from valid Bearer header")
    void shouldExtractTokenFromValidBearerHeader() {
        // given
        String authHeader = "Bearer valid-token-123";

        // when
        String token = tokenUtil.extractAccessToken(authHeader);

        // then
        assertEquals("valid-token-123", token);
    }

    @Test
    @DisplayName("Should extract token with whitespace trimming")
    void shouldExtractTokenWithWhitespaceTrimming() {
        // given
        String authHeader = "Bearer   token-with-spaces   ";

        // when
        String token = tokenUtil.extractAccessToken(authHeader);

        // then
        assertEquals("token-with-spaces", token);
    }

    @Test
    @DisplayName("Should throw exception when header is null")
    void shouldThrowExceptionWhenHeaderIsNull() {
        // given
        String authHeader = null;

        // when & then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> tokenUtil.extractAccessToken(authHeader)
        );
        assertEquals("Authorization header is required", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when header is blank")
    void shouldThrowExceptionWhenHeaderIsBlank() {
        // given
        String authHeader = "   ";

        // when & then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> tokenUtil.extractAccessToken(authHeader)
        );
        assertEquals("Authorization header is required", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when header doesn't start with Bearer")
    void shouldThrowExceptionWhenHeaderDoesNotStartWithBearer() {
        // given
        String authHeader = "Token valid-token-123";

        // when & then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> tokenUtil.extractAccessToken(authHeader)
        );
        assertEquals("Authorization header must start with 'Bearer '", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when token is empty after Bearer")
    void shouldThrowExceptionWhenTokenIsEmptyAfterBearer() {
        // given
        String authHeader = "Bearer ";

        // when & then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> tokenUtil.extractAccessToken(authHeader)
        );
        assertEquals("Access token cannot be empty", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when token is only whitespace")
    void shouldThrowExceptionWhenTokenIsOnlyWhitespace() {
        // given
        String authHeader = "Bearer    ";

        // when & then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> tokenUtil.extractAccessToken(authHeader)
        );
        assertEquals("Access token cannot be empty", exception.getMessage());
    }
}

