package com.vlatkogalev.domain.user.repository

import com.vlatkogalev.domain.user.model.PasswordResetToken
import com.vlatkogalev.domain.user.model.UserAccount
import java.time.Instant


interface UserRepository {
    fun findById(userId: Long): UserAccount?

    fun findByEmail(email: String): UserAccount?

    fun create(email: String, fullName: String, passwordHash: String): UserAccount

    fun saveRefreshToken(userId: Long, token: String, expiresAt: Instant)

    fun revokeRefreshTokensForUser(userId: Long)

    fun upsertPasswordResetToken(userId: Long, token: String, expiresAt: Instant)

    fun findPasswordResetToken(token: String): PasswordResetToken?

    fun consumePasswordResetToken(token: String)

    fun updatePassword(userId: Long, newPasswordHash: String)

    fun confirmPasswordReset(token: String, newPasswordHash: String): PasswordResetConfirmationResult

    fun deleteById(userId: Long): Boolean
}

enum class PasswordResetConfirmationResult {
    SUCCESS,
    INVALID_TOKEN,
    EXPIRED_TOKEN,
}
