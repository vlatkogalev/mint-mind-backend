package com.vlatkogalev.app.api.dto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UserAuthRequestValidationTest {
    @Test
    fun anonymousAuthRequest_blankInstallationId_returnsError() {
        assertEquals("installationId is required", AnonymousAuthRequest(installationId = " ").validate())
    }

    @Test
    fun anonymousAuthRequest_valid_returnsNull() {
        assertNull(AnonymousAuthRequest(installationId = "device-uuid").validate())
    }

    @Test
    fun signupRequest_blankEmail_returnsError() {
        assertEquals("email is required", SignupRequest(email = " ", password = "password").validate())
    }

    @Test
    fun signupRequest_blankPassword_returnsError() {
        assertEquals("password is required", SignupRequest(email = "user@example.com", password = " ").validate())
    }

    @Test
    fun signupRequest_valid_returnsNull() {
        assertNull(SignupRequest(email = "user@example.com", password = "password").validate())
    }

    @Test
    fun loginRequest_blankEmail_returnsError() {
        assertEquals("email is required", LoginRequest(email = " ", password = "password").validate())
    }

    @Test
    fun loginRequest_blankPassword_returnsError() {
        assertEquals("password is required", LoginRequest(email = "user@example.com", password = " ").validate())
    }

    @Test
    fun loginRequest_valid_returnsNull() {
        assertNull(LoginRequest(email = "user@example.com", password = "password").validate())
    }

    @Test
    fun refreshTokenRequest_blankToken_returnsError() {
        assertEquals("refreshToken is required", RefreshTokenRequest(refreshToken = " ").validate())
    }

    @Test
    fun refreshTokenRequest_valid_returnsNull() {
        assertNull(RefreshTokenRequest(refreshToken = "token").validate())
    }

    @Test
    fun confirmPasswordReset_blankToken_returnsError() {
        assertEquals("token is required", ConfirmPasswordResetRequest(token = " ", newPassword = "Pass123!").validate())
    }

    @Test
    fun confirmPasswordReset_blankPassword_returnsError() {
        assertEquals("newPassword is required", ConfirmPasswordResetRequest(token = "token", newPassword = " ").validate())
    }

    @Test
    fun confirmPasswordReset_valid_returnsNull() {
        assertNull(ConfirmPasswordResetRequest(token = "token", newPassword = "Pass123!").validate())
    }
}
