package com.vlatkogalev.domain.user.service

import com.vlatkogalev.domain.user.model.*
import com.vlatkogalev.domain.user.repository.UserRepository
import com.vlatkogalev.platform.core.Result
import java.security.MessageDigest
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import java.time.Duration

private const val RESEND_COOLDOWN_MINUTES = 10L

class UserAuthServiceImpl(
    private val userRepository: UserRepository,
    private val passwordHasher: UserPasswordHasher,
    private val jwtTokenProvider: UserTokenProvider,
    private val skipEmailVerification: Boolean,
    private val emailVerificationSender: EmailVerificationSender,
    private val passwordResetEmailSender: PasswordResetEmailSender,
) : UserAuthService {

    override suspend fun authenticateAnonymous(installationId: String): Result<AuthSession> {
        val normalizedInstallationId = installationId.trim()
        if (normalizedInstallationId.isBlank()) return Result.Failure("installationId is required")
        if (normalizedInstallationId.length > 255) return Result.Failure("installationId must be 255 characters or fewer")

        return try {
            val existing = userRepository.findUserByInstallationId(normalizedInstallationId)
            val user = if (existing != null) {
                userRepository.updateLastSeen(normalizedInstallationId)
                existing
            } else {
                userRepository.createAnonymousUser(normalizedInstallationId)
            }

            // Intentional: always rotate the refresh token on anonymous auth.
            // This means calling this while an existing session is active will
            // invalidate it. Clients should only call this when no valid session exists.
            val refreshToken = jwtTokenProvider.generateRefreshToken(user.id)
            userRepository.saveRefreshTokenHash(user.id, hashToken(refreshToken))
            Result.Success(user.toAuthSession(refreshToken))
        } catch (ex: Exception) {
            Result.Failure(ex.message ?: "Failed to authenticate anonymous user", ex)
        }
    }

    override suspend fun register(email: String, password: String, firstName: String, lastName: String): Result<User> {
        val normalizedEmail = email.trim().lowercase()
        if (!isValidEmail(normalizedEmail)) return Result.Failure("Invalid email")
        if (firstName.isBlank()) return Result.Failure("First name is required")
        if (firstName.trim().length > 50) return Result.Failure("First name must be 50 characters or fewer")
        if (lastName.isBlank()) return Result.Failure("Last name is required")
        if (lastName.trim().length > 50) return Result.Failure("Last name must be 50 characters or fewer")

        validatePassword(password)?.let { return it }

        return try {
            if (userRepository.findAuthIdentityByEmail(normalizedEmail) != null) {
                return Result.Failure("Email already registered")
            }

            val verificationToken = UUID.randomUUID().toString()
            val passwordHash = passwordHasher.hash(password)

            val user = userRepository.create(
                email = normalizedEmail,
                firstName = firstName.trim(),
                lastName = lastName.trim(),
                passwordHash = passwordHash,
                verificationToken = verificationToken,
            )

            userRepository.createAuthIdentity(
                UserAuthIdentity(
                    id = UUID.randomUUID(),
                    userId = user.id,
                    authType = AuthType.EMAIL,
                    email = normalizedEmail,
                    passwordHash = passwordHash,
                    createdAt = Instant.now(),
                ),
            )

            if (skipEmailVerification) {
                userRepository.verifyEmail(verificationToken)
            } else {
                emailVerificationSender.sendVerificationEmail(normalizedEmail, verificationToken)
            }

            Result.Success(user.toUser())
        } catch (ex: Exception) {
            Result.Failure(ex.message ?: "Failed to register", ex)
        }
    }

    override suspend fun signup(email: String, password: String, currentUserId: UUID): Result<AuthSession> {
        val normalizedEmail = email.trim().lowercase()
        if (!isValidEmail(normalizedEmail)) return Result.Failure("Invalid email")
        validatePassword(password)?.let { return it }

        return try {
            if (userRepository.findAuthIdentityByEmail(normalizedEmail) != null) {
                return Result.Failure("Email already registered")
            }

            val passwordHash = passwordHasher.hash(password)
            val verificationToken = UUID.randomUUID().toString()

            val user = when (val result = userRepository.upgradeAnonymousUser(
                userId = currentUserId,
                email = normalizedEmail,
                passwordHash = passwordHash,
                verificationToken = verificationToken,
                markVerified = skipEmailVerification,
            )) {
                is UpgradeAnonymousResult.NotFound -> return Result.Failure("User not found")
                is UpgradeAnonymousResult.NotAnonymous -> return Result.Failure("Signup upgrade requires anonymous account")
                is UpgradeAnonymousResult.Success -> result.user
            }

            if (skipEmailVerification) {
                userRepository.verifyEmail(verificationToken)
            } else {
                emailVerificationSender.sendVerificationEmail(normalizedEmail, verificationToken)
            }

            val refreshedUser = userRepository.findById(user.id) ?: user
            val refreshToken = jwtTokenProvider.generateRefreshToken(refreshedUser.id)
            userRepository.saveRefreshTokenHash(refreshedUser.id, hashToken(refreshToken))
            Result.Success(refreshedUser.toAuthSession(refreshToken))
        } catch (ex: Exception) {
            Result.Failure(ex.message ?: "Failed to signup", ex)
        }
    }

    override suspend fun login(email: String, password: String): Result<LoginSession> {
        val normalizedEmail = email.trim().lowercase()
        return try {
            val identity = userRepository.findAuthIdentityByEmail(normalizedEmail)
                ?: return Result.Failure("Invalid email or password")
            if (identity.authType != AuthType.EMAIL || identity.passwordHash.isNullOrBlank()) {
                return Result.Failure("Invalid email or password")
            }
            if (!passwordHasher.verify(password, identity.passwordHash)) {
                return Result.Failure("Invalid email or password")
            }
            val user = userRepository.findById(identity.userId)
                ?: return Result.Failure("Invalid email or password")
            if (!user.emailVerified) {
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

    override suspend fun refresh(refreshToken: String): Result<LoginSession> =
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

    override suspend fun verifyEmail(token: String): Result<Unit> =
        try {
            if (token.isBlank()) return Result.Failure("Verification token is required")
            if (userRepository.verifyEmail(token)) Result.Success(Unit) else Result.Failure("Invalid verification token")
        } catch (ex: Exception) {
            Result.Failure(ex.message ?: "Failed to verify email", ex)
        }

    override suspend fun resendVerification(email: String): Result<Unit> {
        val normalizedEmail = email.trim().lowercase()
        return try {
            val identity = userRepository.findAuthIdentityByEmail(normalizedEmail)
            val user = identity?.let { userRepository.findById(it.userId) }

            if (user == null || user.emailVerified) {
                return Result.Success(Unit)
            }

            val lastSent = user.verificationEmailSentAt
            if (lastSent != null) {
                val minutesSince = Duration.between(lastSent, Instant.now()).toMinutes()
                if (minutesSince < RESEND_COOLDOWN_MINUTES) {
                    val waitMinutes = RESEND_COOLDOWN_MINUTES - minutesSince
                    return Result.Failure("Please wait $waitMinutes more minute(s) before requesting another verification email")
                }
            }

            val newToken = UUID.randomUUID().toString()
            userRepository.updateVerificationToken(user.id, newToken)
            emailVerificationSender.sendVerificationEmail(normalizedEmail, newToken)

            Result.Success(Unit)
        } catch (ex: Exception) {
            Result.Failure(ex.message ?: "Failed to resend verification email", ex)
        }
    }

    override suspend fun getUserProfile(userId: UUID): Result<User> =
        try {
            val user = userRepository.findById(userId) ?: return Result.Failure("User not found")
            Result.Success(user.toUser())
        } catch (ex: Exception) {
            Result.Failure(ex.message ?: "Failed to fetch user profile", ex)
        }

    override suspend fun updateProfile(userId: UUID, firstName: String, lastName: String): Result<User> {
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

    override suspend fun requestPasswordReset(email: String): Result<Unit> {
        val normalizedEmail = email.trim().lowercase()
        return try {
            val user = userRepository.findAuthIdentityByEmail(normalizedEmail)
                ?.let { userRepository.findById(it.userId) }

            if (user != null) {
                val token = UUID.randomUUID().toString()
                userRepository.upsertPasswordResetToken(
                    userId = user.id,
                    token = token,
                    expiresAt = Instant.now().plus(15, ChronoUnit.MINUTES),
                )
                passwordResetEmailSender.sendPasswordResetEmail(normalizedEmail, token)
            }

            Result.Success(Unit)
        } catch (ex: Exception) {
            Result.Failure(ex.message ?: "Failed to request password reset", ex)
        }
    }

    override suspend fun confirmPasswordReset(token: String, newPassword: String): Result<Unit> {
        return try {
            val resetToken = userRepository.findPasswordResetToken(token)
                ?: return Result.Failure("Invalid reset token")

            if (resetToken.expiresAt.isBefore(Instant.now())) {
                return Result.Failure("Reset token expired")
            }

            validatePassword(newPassword)?.let { return it }

            when (userRepository.confirmPasswordReset(token, passwordHasher.hash(newPassword))) {
                PasswordResetConfirmationResult.SUCCESS -> Result.Success(Unit)
                PasswordResetConfirmationResult.INVALID_TOKEN -> Result.Failure("Invalid reset token")
                PasswordResetConfirmationResult.EXPIRED_TOKEN -> Result.Failure("Reset token expired")
            }
        } catch (ex: Exception) {
            Result.Failure(ex.message ?: "Failed to reset password", ex)
        }
    }

    override suspend fun deleteAccount(userId: UUID): Result<Unit> =
        try {
            if (userRepository.deleteById(userId)) Result.Success(Unit) else Result.Failure("User not found")
        } catch (ex: Exception) {
            Result.Failure(ex.message ?: "Failed to delete account", ex)
        }

    override suspend fun logout(userId: UUID): Result<Unit> = try {
        userRepository.clearRefreshTokenHash(userId)
        Result.Success(Unit)
    } catch (ex: Exception) {
        Result.Failure(ex.message ?: "Failed to logout", ex)
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
            isAnonymous = isAnonymous,
            upgradedAt = upgradedAt,
        )
    }

    private fun UserAccount.toLoginSession(refreshToken: String): LoginSession =
        LoginSession(
            accessToken = jwtTokenProvider.createAccessToken(id, isAnonymous),
            refreshToken = refreshToken,
            accessTokenExpiresInSeconds = jwtTokenProvider.accessTokenExpiresInSeconds(),
            refreshTokenExpiresInSeconds = jwtTokenProvider.refreshTokenExpiresInSeconds(),
        )

    private fun UserAccount.toAuthSession(refreshToken: String): AuthSession =
        AuthSession(
            accessToken = jwtTokenProvider.createAccessToken(id, isAnonymous),
            refreshToken = refreshToken,
            accessTokenExpiresInSeconds = jwtTokenProvider.accessTokenExpiresInSeconds(),
            refreshTokenExpiresInSeconds = jwtTokenProvider.refreshTokenExpiresInSeconds(),
            user = toUser(),
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