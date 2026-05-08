package com.vlatkogalev.app.data.repository

import com.vlatkogalev.app.data.entities.PasswordResetTokenRecord
import com.vlatkogalev.app.data.entities.UserRecord
import java.time.Instant


interface UserRepository {
    fun findById(userId: Long): UserRecord?

    fun findByEmail(email: String): UserRecord?

    fun create(email: String, fullName: String, passwordHash: String): UserRecord

    fun saveRefreshToken(userId: Long, token: String, expiresAt: Instant)

    fun revokeRefreshTokensForUser(userId: Long)

    fun upsertPasswordResetToken(userId: Long, token: String, expiresAt: Instant)

    fun findPasswordResetToken(token: String): PasswordResetTokenRecord?

    fun consumePasswordResetToken(token: String)

    fun updatePassword(userId: Long, newPasswordHash: String)

    fun deleteById(userId: Long): Boolean
}
