package com.vlatkogalev.domain.user.service

import com.vlatkogalev.domain.user.model.AuthSession
import com.vlatkogalev.domain.user.model.LoginSession
import com.vlatkogalev.domain.user.model.User
import com.vlatkogalev.platform.core.Result
import java.util.*

interface UserAuthService {
    suspend fun register(email: String, password: String, firstName: String, lastName: String): Result<User>

    suspend fun authenticateAnonymous(installationId: String): Result<AuthSession>

    suspend fun signup(email: String, password: String, firstName: String, lastName: String, currentUserId: UUID): Result<AuthSession>

    suspend fun login(email: String, password: String): Result<LoginSession>

    suspend fun refresh(refreshToken: String): Result<LoginSession>

    suspend fun verifyEmail(token: String): Result<Unit>

    suspend fun resendVerification(email: String): Result<Unit>

    suspend fun getUserProfile(userId: UUID): Result<User>

    suspend fun updateProfile(userId: UUID, firstName: String, lastName: String): Result<User>

    suspend fun requestPasswordReset(email: String): Result<Unit>

    suspend fun confirmPasswordReset(token: String, newPassword: String): Result<Unit>

    suspend fun deleteAccount(userId: UUID): Result<Unit>

    suspend fun logout(userId: UUID): Result<Unit>
}
