package com.vlatkogalev.app.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val fullName: String,
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class RequestPasswordResetRequest(
    val email: String,
)

@Serializable
data class ConfirmPasswordResetRequest(
    val token: String,
    val newPassword: String,
)

@Serializable
data class DeleteAccountRequest(
    val confirm: Boolean,
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
    val id: Long,
    val email: String,
    val fullName: String,
)

@Serializable
data class PasswordResetRequestResponse(
    val resetToken: String?,
    val message: String,
)
