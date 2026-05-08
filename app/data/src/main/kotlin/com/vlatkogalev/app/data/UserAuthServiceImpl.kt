package com.vlatkogalev.app.data

import com.vlatkogalev.app.data.entities.UserRecord
import com.vlatkogalev.app.data.repository.UserRepository
import com.vlatkogalev.app.domain.model.User
import com.vlatkogalev.app.domain.service.LoginSession
import com.vlatkogalev.app.domain.service.PasswordResetRequestResult
import com.vlatkogalev.app.domain.service.UserAuthService
import com.vlatkogalev.platform.auth.JwtTokenProvider
import com.vlatkogalev.platform.auth.PasswordHasher
import com.vlatkogalev.platform.core.Result
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class UserAuthServiceImpl(
    private val userRepository: UserRepository,
    private val passwordHasher: PasswordHasher,
    private val jwtTokenProvider: JwtTokenProvider,
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
            val resetToken = userRepository.findPasswordResetToken(token)
                ?: return Result.Failure("Invalid reset token")

            if (resetToken.expiresAt.isBefore(Instant.now())) {
                userRepository.consumePasswordResetToken(token)
                Result.Failure("Reset token expired")
            } else {
                userRepository.updatePassword(resetToken.userId, passwordHasher.hash(newPassword))
                userRepository.consumePasswordResetToken(token)
                userRepository.revokeRefreshTokensForUser(resetToken.userId)
                Result.Success(Unit)
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

    private fun UserRecord.toUser(): User = User(id = id, email = email, fullName = fullName)

    private fun UserRecord.toLoginSession(refreshToken: String): LoginSession =
        LoginSession(
            accessToken = jwtTokenProvider.createAccessToken(id, email),
            refreshToken = refreshToken,
            accessTokenExpiresInSeconds = jwtTokenProvider.accessTokenExpiresInSeconds(),
            refreshTokenExpiresInSeconds = jwtTokenProvider.refreshTokenExpiresInSeconds(),
        )
}
