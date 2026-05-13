package com.vlatkogalev.domain.user.service

import com.vlatkogalev.domain.user.model.LoginSession
import com.vlatkogalev.domain.user.model.PasswordResetRequestResult
import com.vlatkogalev.domain.user.model.User
import com.vlatkogalev.platform.core.Result
import java.util.UUID

interface UserAuthService {
    fun register(email: String, password: String, firstName: String, lastName: String): Result<User>

    fun login(email: String, password: String): Result<LoginSession>

    fun refresh(refreshToken: String): Result<LoginSession>

    fun verifyEmail(token: String): Result<Unit>

    fun resendVerification(email: String): Result<Unit>

    fun logout(userId: UUID): Result<Unit>

    fun getUserProfile(userId: UUID): Result<User>

    fun requestPasswordReset(email: String): Result<PasswordResetRequestResult>

    fun confirmPasswordReset(token: String, newPassword: String): Result<Unit>

    fun deleteAccount(userId: UUID): Result<Unit>
}
