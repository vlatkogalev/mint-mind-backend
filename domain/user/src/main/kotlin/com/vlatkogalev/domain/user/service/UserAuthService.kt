package com.vlatkogalev.domain.user.service

import com.vlatkogalev.domain.user.model.LoginSession
import com.vlatkogalev.domain.user.model.PasswordResetRequestResult
import com.vlatkogalev.domain.user.model.User
import com.vlatkogalev.platform.core.Result

interface UserAuthService {
    fun register(email: String, password: String, fullName: String): Result<Unit>

    fun login(email: String, password: String): Result<LoginSession>

    fun getUserProfile(userId: Long): Result<User>

    fun requestPasswordReset(email: String): Result<PasswordResetRequestResult>

    fun confirmPasswordReset(token: String, newPassword: String): Result<Unit>

    fun deleteAccount(userId: Long): Result<Unit>
}
