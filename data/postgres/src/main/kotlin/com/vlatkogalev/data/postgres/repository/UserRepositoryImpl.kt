package com.vlatkogalev.data.postgres.repository

import com.vlatkogalev.data.postgres.daos.UserQueries
import com.vlatkogalev.data.postgres.entities.PasswordResetTokenRecord
import com.vlatkogalev.data.postgres.entities.UserRecord
import com.vlatkogalev.domain.user.model.PasswordResetToken
import com.vlatkogalev.domain.user.model.UserAccount
import com.vlatkogalev.domain.user.repository.PasswordResetConfirmationResult
import com.vlatkogalev.domain.user.repository.UserRepository
import com.vlatkogalev.platform.database.withTransaction
import java.time.Instant
import javax.sql.DataSource

class UserRepositoryImpl(
    private val queries: UserQueries,
    private val dataSource: DataSource,
) : UserRepository {
    override fun findById(userId: Long): UserAccount? = queries.findById(userId)?.toUserAccount()

    override fun findByEmail(email: String): UserAccount? = queries.findByEmail(email)?.toUserAccount()

    override fun create(email: String, fullName: String, passwordHash: String): UserAccount =
        queries.create(email, fullName, passwordHash).toUserAccount()

    override fun saveRefreshToken(userId: Long, token: String, expiresAt: Instant) {
        queries.saveRefreshToken(userId, token, expiresAt)
    }

    override fun revokeRefreshTokensForUser(userId: Long) {
        queries.revokeRefreshTokensForUser(userId)
    }

    override fun upsertPasswordResetToken(userId: Long, token: String, expiresAt: Instant) {
        queries.upsertPasswordResetToken(userId, token, expiresAt)
    }

    override fun findPasswordResetToken(token: String): PasswordResetToken? =
        queries.findPasswordResetToken(token)?.toPasswordResetToken()

    override fun consumePasswordResetToken(token: String) {
        queries.consumePasswordResetToken(token)
    }

    override fun updatePassword(userId: Long, newPasswordHash: String) {
        queries.updatePassword(userId, newPasswordHash)
    }

    override fun confirmPasswordReset(
        token: String,
        newPasswordHash: String,
    ): PasswordResetConfirmationResult =
        dataSource.withTransaction { connection ->
            val resetToken = queries.findPasswordResetToken(connection, token)
                ?: return@withTransaction PasswordResetConfirmationResult.INVALID_TOKEN

            if (resetToken.expiresAt.isBefore(Instant.now())) {
                queries.consumePasswordResetToken(connection, token)
                return@withTransaction PasswordResetConfirmationResult.EXPIRED_TOKEN
            }

            queries.updatePassword(connection, resetToken.userId, newPasswordHash)
            queries.consumePasswordResetToken(connection, token)
            queries.revokeRefreshTokensForUser(connection, resetToken.userId)
            PasswordResetConfirmationResult.SUCCESS
        }

    override fun deleteById(userId: Long): Boolean = queries.deleteById(userId)

    private fun UserRecord.toUserAccount(): UserAccount =
        UserAccount(
            id = id,
            email = email,
            fullName = fullName,
            passwordHash = passwordHash,
        )

    private fun PasswordResetTokenRecord.toPasswordResetToken(): PasswordResetToken =
        PasswordResetToken(
            userId = userId,
            token = token,
            expiresAt = expiresAt,
        )
}
