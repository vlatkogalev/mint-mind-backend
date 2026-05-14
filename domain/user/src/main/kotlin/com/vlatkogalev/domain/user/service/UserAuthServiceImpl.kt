package com.vlatkogalev.domain.user.service

import com.vlatkogalev.domain.user.model.*
import com.vlatkogalev.domain.user.repository.UserRepository
import com.vlatkogalev.platform.core.Result
import java.security.MessageDigest
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

class UserAuthServiceImpl(
    private val userRepository: UserRepository,
    private val passwordHasher: UserPasswordHasher,
    private val jwtTokenProvider: UserTokenProvider,
    private val emailVerificationSender: EmailVerificationSender,
) : UserAuthService {
    override fun register(email: String, password: String, firstName: String, lastName: String): Result<User> {
        val normalizedEmail = email.trim().lowercase()
        if (!isValidEmail(normalizedEmail)) return Result.Failure("Invalid email")
        if (firstName.isBlank()) return Result.Failure("First name is required")
        if (firstName.trim().length > 50) return Result.Failure("First name must be 50 characters or fewer")
        if (lastName.isBlank()) return Result.Failure("Last name is required")
        if (lastName.trim().length > 50) return Result.Failure("Last name must be 50 characters or fewer")

        validatePassword(password)?.let { return it }

        return try {
            if (userRepository.findByEmail(normalizedEmail) != null) {
                Result.Failure("Email already registered")
            } else {
                val verificationToken = UUID.randomUUID().toString()
                val user = userRepository.create(
                    email = normalizedEmail,
                    firstName = firstName.trim(),
                    lastName = lastName.trim(),
                    passwordHash = passwordHasher.hash(password),
                    verificationToken = verificationToken,
                )
                emailVerificationSender.sendVerificationEmail(normalizedEmail, verificationToken)
                Result.Success(user.toUser())
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
            } else if (!user.emailVerified) {
                Result.Failure("Email verification required")
            } else {
                val refreshToken = jwtTokenProvider.generateRefreshToken(user.id)
                userRepository.saveRefreshTokenHash(
                    userId = user.id,
                    tokenHash = hashToken(refreshToken),
                )
                Result.Success(user.toLoginSession(refreshToken))
            }
        } catch (ex: Exception) {
            Result.Failure(ex.message ?: "Failed to login", ex)
        }
    }

    override fun refresh(refreshToken: String): Result<LoginSession> =
        try {
            val parsedUserId = runCatching { UUID.fromString(refreshToken.substringBefore(':')) }.getOrNull()
                ?: return Result.Failure("Invalid refresh token")
            val rawToken = refreshToken.substringAfter(':', missingDelimiterValue = "")
            if (rawToken.isBlank()) return Result.Failure("Invalid refresh token")

            val user = userRepository.findById(parsedUserId) ?: return Result.Failure("Invalid refresh token")
            val storedTokenHash = user.refreshTokenHash ?: return Result.Failure("Invalid refresh token")
            if (!constantTimeEquals(hashToken(refreshToken), storedTokenHash)) {
                return Result.Failure("Invalid refresh token")
            }

            val rotatedRefreshToken = jwtTokenProvider.generateRefreshToken(user.id)
            userRepository.saveRefreshTokenHash(user.id, hashToken(rotatedRefreshToken))
            Result.Success(user.toLoginSession(rotatedRefreshToken))
        } catch (ex: Exception) {
            Result.Failure(ex.message ?: "Failed to refresh token", ex)
        }

    override fun verifyEmail(token: String): Result<Unit> =
        try {
            if (token.isBlank()) return Result.Failure("Verification token is required")
            if (userRepository.verifyEmail(token)) Result.Success(Unit) else Result.Failure("Invalid verification token")
        } catch (ex: Exception) {
            Result.Failure(ex.message ?: "Failed to verify email", ex)
        }

    override fun resendVerification(email: String): Result<Unit> {
        val normalizedEmail = email.trim().lowercase()
        return try {
            val user = userRepository.findByEmail(normalizedEmail)

            if (user == null || user.emailVerified) {
                return Result.Success(Unit)
            }

            val newToken = UUID.randomUUID().toString()
            userRepository.updateVerificationToken(user.id, newToken)
            emailVerificationSender.sendVerificationEmail(normalizedEmail, newToken)

            Result.Success(Unit)
        } catch (ex: Exception) {
            Result.Failure(ex.message ?: "Failed to resend verification email", ex)
        }
    }

    override fun getUserProfile(userId: UUID): Result<User> =
        try {
            val user = userRepository.findById(userId) ?: return Result.Failure("User not found")
            Result.Success(user.toUser())
        } catch (ex: Exception) {
            Result.Failure(ex.message ?: "Failed to fetch user profile", ex)
        }

    override fun updateProfile(userId: UUID, firstName: String, lastName: String): Result<User> {
        if (firstName.isBlank()) return Result.Failure("First name is required")
        if (lastName.isBlank()) return Result.Failure("Last name is required")
        if (firstName.trim().length > 50) return Result.Failure("First name must be 50 characters or fewer")
        if (lastName.trim().length > 50) return Result.Failure("Last name must be 50 characters or fewer")
        return try {
            val user = userRepository.updateProfile(userId, firstName.trim(), lastName.trim())
                ?: return Result.Failure("User not found")
            Result.Success(user.toUser())
        } catch (ex: Exception) {
            Result.Failure(ex.message ?: "Failed to update profile", ex)
        }
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
        validatePassword(newPassword)?.let { return it }

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

    override fun deleteAccount(userId: UUID): Result<Unit> =
        try {
            if (userRepository.deleteById(userId)) Result.Success(Unit) else Result.Failure("User not found")
        } catch (ex: Exception) {
            Result.Failure(ex.message ?: "Failed to delete account", ex)
        }

    private fun UserAccount.toUser(): User {
        val profile = profile ?: error("User profile is missing")
        return User(
            id = id,
            email = email,
            firstName = profile.firstName,
            lastName = profile.lastName,
            avatarUrl = profile.avatarUrl,
            emailVerified = emailVerified,
        )
    }

    private fun UserAccount.toLoginSession(refreshToken: String): LoginSession =
        LoginSession(
            accessToken = jwtTokenProvider.createAccessToken(id, email),
            refreshToken = refreshToken,
            accessTokenExpiresInSeconds = jwtTokenProvider.accessTokenExpiresInSeconds(),
            refreshTokenExpiresInSeconds = jwtTokenProvider.refreshTokenExpiresInSeconds(),
        )

    private fun hashToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(token.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(digest)
    }

    private fun constantTimeEquals(left: String, right: String): Boolean =
        MessageDigest.isEqual(left.toByteArray(Charsets.UTF_8), right.toByteArray(Charsets.UTF_8))

    private fun isValidEmail(email: String): Boolean = EMAIL_REGEX.matches(email)

    private fun validatePassword(password: String): Result.Failure? {
        if (password.length < 8) return Result.Failure("Password must be at least 8 characters")
        if (!password.any { it.isUpperCase() }) return Result.Failure("Password must contain at least one uppercase letter")
        if (!password.any { it.isLowerCase() }) return Result.Failure("Password must contain at least one lowercase letter")
        if (!password.any { it.isDigit() }) return Result.Failure("Password must contain at least one digit")
        if (!password.any { !it.isLetterOrDigit() }) return Result.Failure("Password must contain at least one special character")
        return null
    }

    companion object {
        private val EMAIL_REGEX = Regex("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", RegexOption.IGNORE_CASE)
    }
}
