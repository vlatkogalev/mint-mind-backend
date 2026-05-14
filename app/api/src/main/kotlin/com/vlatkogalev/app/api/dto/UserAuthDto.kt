package com.vlatkogalev.app.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String,
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class RefreshTokenRequest(
    val refreshToken: String,
)

@Serializable
data class RequestPasswordResetRequest(
    val email: String,
)

@Serializable
data class ResendVerificationRequest(
    val email: String,
)

@Serializable
data class ConfirmPasswordResetRequest(
    val token: String,
    val newPassword: String,
)

@Serializable
data class UpdateProfileRequest(
    val firstName: String,
    val lastName: String,
)

@Serializable
data class LoginSessionResponse(
    val accessToken: String,
    val refreshToken: String,
    val accessTokenExpiresInSeconds: Long,
    val refreshTokenExpiresInSeconds: Long,
)

@Serializable
data class UserResponse(
    val id: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val avatarUrl: String?,
    val emailVerified: Boolean,
)

@Serializable
data class PasswordResetRequestResponse(
    val resetToken: String?,
    val message: String,
)
