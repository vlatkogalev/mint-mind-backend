package com.vlatkogalev.domain.user.service

import com.vlatkogalev.domain.user.model.AuthSession
import com.vlatkogalev.domain.user.model.LoginSession
import com.vlatkogalev.domain.user.model.User
import com.vlatkogalev.platform.core.Result
import java.util.*

interface UserAuthService {
    fun register(email: String, password: String, firstName: String, lastName: String): Result<User>

    fun authenticateAnonymous(installationId: String): Result<AuthSession>

    fun signup(email: String, password: String, currentUserId: UUID): Result<AuthSession>

    fun login(email: String, password: String): Result<LoginSession>

    fun refresh(refreshToken: String): Result<LoginSession>

    fun verifyEmail(token: String): Result<Unit>

    fun resendVerification(email: String): Result<Unit>

    fun getUserProfile(userId: UUID): Result<User>

    fun updateProfile(userId: UUID, firstName: String, lastName: String): Result<User>

    fun requestPasswordReset(email: String): Result<Unit>

    fun confirmPasswordReset(token: String, newPassword: String): Result<Unit>

    fun deleteAccount(userId: UUID): Result<Unit>
}
