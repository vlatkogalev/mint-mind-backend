package com.vlatkogalev.app.domain.service

import com.vlatkogalev.app.domain.model.User
import com.vlatkogalev.platform.core.Result

data class LoginSession(
    val accessToken: String,
    val refreshToken: String,
    val accessTokenExpiresInSeconds: Long,
    val refreshTokenExpiresInSeconds: Long,
)

data class PasswordResetRequestResult(
    val resetToken: String,
)

interface UserAuthService {
    fun register(email: String, password: String, fullName: String): Result<Unit>

    fun login(email: String, password: String): Result<LoginSession>

    fun getUserProfile(userId: Long): Result<User>

    fun requestPasswordReset(email: String): Result<PasswordResetRequestResult>

    fun confirmPasswordReset(token: String, newPassword: String): Result<Unit>

    fun deleteAccount(userId: Long): Result<Unit>
}
