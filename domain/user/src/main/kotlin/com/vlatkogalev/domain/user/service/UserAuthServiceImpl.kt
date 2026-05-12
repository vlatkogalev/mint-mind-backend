package com.vlatkogalev.domain.user.service

import com.vlatkogalev.domain.user.model.LoginSession
import com.vlatkogalev.domain.user.model.PasswordResetRequestResult
import com.vlatkogalev.domain.user.model.User
import com.vlatkogalev.domain.user.model.UserAccount
import com.vlatkogalev.domain.user.repository.PasswordResetConfirmationResult
import com.vlatkogalev.domain.user.repository.UserRepository
import com.vlatkogalev.platform.core.Result
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class UserAuthServiceImpl(
    private val userRepository: UserRepository,
    private val passwordHasher: UserPasswordHasher,
    private val jwtTokenProvider: UserTokenProvider,
) : UserAuthService {
    override fun register(email: String, password: String, fullName: String): Result<Unit> {
        val normalizedEmail = email.trim().lowercase()
        if (!normalizedEmail.contains('@')) return Result.Failure("Invalid email")
        if (fullName.isBlank()) return Result.Failure("Full name is required")
        if (password.length < 8) return Result.Failure("Password must be at least 8 characters")

        return try {
            if (userRepository.findByEmail(normalizedEmail) != null) {
                Result.Failure("Email already registered")
            } else {
                userRepository.create(
                    email = normalizedEmail,
                    fullName = fullName.trim(),
                    passwordHash = passwordHasher.hash(password),
                )
                Result.Success(Unit)
            }
        } catch (ex: Exception) {
            Result.Failure(ex.message ?: "Failed to register", ex)
        }
    }

    override fun login(email: String, password: String): Result<LoginSession> {
        val normalizedEmail = email.trim().lowercase()
        return try {
            val user = userRepository.findByEmail(normalizedEmail)
                ?: return Result.Failure("Invalid email or password")

            if (!passwordHasher.verify(password, user.passwordHash)) {
                Result.Failure("Invalid email or password")
            } else {
                val refreshToken = jwtTokenProvider.generateRefreshToken()
                userRepository.saveRefreshToken(
                    userId = user.id,
                    token = refreshToken,
                    expiresAt = Instant.now().plus(jwtTokenProvider.refreshTokenExpiresInSeconds(), ChronoUnit.SECONDS),
                )
                Result.Success(user.toLoginSession(refreshToken))
            }
        } catch (ex: Exception) {
            Result.Failure(ex.message ?: "Failed to login", ex)
        }
    }

    override fun getUserProfile(userId: Long): Result<User> =
        try {
            val user = userRepository.findById(userId) ?: return Result.Failure("User not found")
            Result.Success(user.toUser())
        } catch (ex: Exception) {
            Result.Failure(ex.message ?: "Failed to fetch user profile", ex)
        }

    override fun requestPasswordReset(email: String): Result<PasswordResetRequestResult> {
        val normalizedEmail = email.trim().lowercase()
        return try {
            val user = userRepository.findByEmail(normalizedEmail)
                ?: return Result.Success(PasswordResetRequestResult(resetToken = ""))

            val token = UUID.randomUUID().toString()
            userRepository.upsertPasswordResetToken(
                userId = user.id,
                token = token,
                expiresAt = Instant.now().plus(15, ChronoUnit.MINUTES),
            )
            Result.Success(PasswordResetRequestResult(resetToken = token))
        } catch (ex: Exception) {
            Result.Failure(ex.message ?: "Failed to request password reset", ex)
        }
    }

    override fun confirmPasswordReset(token: String, newPassword: String): Result<Unit> {
        if (newPassword.length < 8) return Result.Failure("Password must be at least 8 characters")

        return try {
            when (userRepository.confirmPasswordReset(token, passwordHasher.hash(newPassword))) {
                PasswordResetConfirmationResult.SUCCESS -> Result.Success(Unit)
                PasswordResetConfirmationResult.INVALID_TOKEN -> Result.Failure("Invalid reset token")
                PasswordResetConfirmationResult.EXPIRED_TOKEN -> Result.Failure("Reset token expired")
            }
        } catch (ex: Exception) {
            Result.Failure(ex.message ?: "Failed to reset password", ex)
        }
    }

    override fun deleteAccount(userId: Long): Result<Unit> =
        try {
            if (userRepository.deleteById(userId)) Result.Success(Unit) else Result.Failure("User not found")
        } catch (ex: Exception) {
            Result.Failure(ex.message ?: "Failed to delete account", ex)
        }

    private fun UserAccount.toUser(): User = User(id = id, email = email, fullName = fullName)

    private fun UserAccount.toLoginSession(refreshToken: String): LoginSession =
        LoginSession(
            accessToken = jwtTokenProvider.createAccessToken(id, email),
            refreshToken = refreshToken,
            accessTokenExpiresInSeconds = jwtTokenProvider.accessTokenExpiresInSeconds(),
            refreshTokenExpiresInSeconds = jwtTokenProvider.refreshTokenExpiresInSeconds(),
        )
}
