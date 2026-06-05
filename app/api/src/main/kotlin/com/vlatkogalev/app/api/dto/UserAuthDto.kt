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
data class AnonymousAuthRequest(
    val installationId: String,
) {
    fun validate(): String? {
        if (installationId.isBlank()) return "installationId is required"
        return null
    }
}

@Serializable
data class SignupRequest(
    val email: String,
    val password: String,
) {
    fun validate(): String? {
        if (email.isBlank()) return "email is required"
        if (password.isBlank()) return "password is required"
        return null
    }
}

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
    val installationId: String? = null,
) {
    fun validate(): String? {
        if (email.isBlank()) return "email is required"
        if (password.isBlank()) return "password is required"
        return null
    }
}

@Serializable
data class RefreshTokenRequest(
    val refreshToken: String,
) {
    fun validate(): String? {
        if (refreshToken.isBlank()) return "refreshToken is required"
        return null
    }
}

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
) {
    fun validate(): String? {
        if (token.isBlank()) return "token is required"
        if (newPassword.isBlank()) return "newPassword is required"
        return null
    }
}

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
data class AuthSessionResponse(
    val accessToken: String,
    val refreshToken: String,
    val accessTokenExpiresInSeconds: Long,
    val refreshTokenExpiresInSeconds: Long,
    val user: UserResponse,
)

@Serializable
data class UserResponse(
    val id: String,
    val email: String?,
    val firstName: String,
    val lastName: String,
    val avatarUrl: String?,
    val emailVerified: Boolean,
    val isAnonymous: Boolean,
    val upgradedAt: Long?,
)

@Serializable
data class PasswordResetRequestResponse(
    val message: String,
)
