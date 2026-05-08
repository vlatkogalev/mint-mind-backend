package com.vlatkogalev.app.data.repository

import com.vlatkogalev.app.data.daos.UserQueries
import com.vlatkogalev.app.data.entities.PasswordResetTokenRecord
import com.vlatkogalev.app.data.entities.UserRecord
import java.time.Instant

class UserRepositoryImpl(
    private val queries: UserQueries,
) : UserRepository {
    override fun findById(userId: Long): UserRecord? = queries.findById(userId)

    override fun findByEmail(email: String): UserRecord? = queries.findByEmail(email)

    override fun create(email: String, fullName: String, passwordHash: String): UserRecord =
        queries.create(email, fullName, passwordHash)

    override fun saveRefreshToken(userId: Long, token: String, expiresAt: Instant) {
        queries.saveRefreshToken(userId, token, expiresAt)
    }

    override fun revokeRefreshTokensForUser(userId: Long) {
        queries.revokeRefreshTokensForUser(userId)
    }

    override fun upsertPasswordResetToken(userId: Long, token: String, expiresAt: Instant) {
        queries.upsertPasswordResetToken(userId, token, expiresAt)
    }

    override fun findPasswordResetToken(token: String): PasswordResetTokenRecord? =
        queries.findPasswordResetToken(token)

    override fun consumePasswordResetToken(token: String) {
        queries.consumePasswordResetToken(token)
    }

    override fun updatePassword(userId: Long, newPasswordHash: String) {
        queries.updatePassword(userId, newPasswordHash)
    }

    override fun deleteById(userId: Long): Boolean = queries.deleteById(userId)
}
