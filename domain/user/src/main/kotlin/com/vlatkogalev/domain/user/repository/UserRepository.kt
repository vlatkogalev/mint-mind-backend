package com.vlatkogalev.domain.user.repository

import com.vlatkogalev.domain.user.model.PasswordResetConfirmationResult
import com.vlatkogalev.domain.user.model.PasswordResetToken
import com.vlatkogalev.domain.user.model.UserAccount
import java.time.Instant
import java.util.UUID

interface UserRepository {
    fun findById(userId: UUID): UserAccount?

    fun findByEmail(email: String): UserAccount?

    fun findByVerificationToken(token: String): UserAccount?

    fun create(
        email: String,
        firstName: String,
        lastName: String,
        passwordHash: String,
        verificationToken: String,
    ): UserAccount

    fun saveRefreshTokenHash(userId: UUID, tokenHash: String)

    fun clearRefreshTokenHash(userId: UUID)

    fun verifyEmail(token: String): Boolean

    fun updateVerificationToken(userId: UUID, token: String)

    fun upsertPasswordResetToken(userId: UUID, token: String, expiresAt: Instant)

    fun findPasswordResetToken(token: String): PasswordResetToken?

    fun consumePasswordResetToken(token: String)

    fun updatePassword(userId: UUID, newPasswordHash: String)

    fun confirmPasswordReset(token: String, newPasswordHash: String): PasswordResetConfirmationResult

    fun deleteById(userId: UUID): Boolean
}
