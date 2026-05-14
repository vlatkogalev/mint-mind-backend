package com.vlatkogalev.data.postgres.daos

import com.vlatkogalev.data.postgres.entities.PasswordResetTokenRecord
import com.vlatkogalev.data.postgres.entities.UserRecord
import java.sql.Connection
import java.sql.ResultSet
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*
import javax.sql.DataSource

class UserQueries(
    private val dataSource: DataSource,
) {
    fun findById(userId: UUID): UserRecord? =
        dataSource.connection.use { connection ->
            findById(connection, userId)
        }

    fun findByEmail(email: String): UserRecord? =
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                SELECT ${userColumns()}
                FROM users u
                LEFT JOIN profiles p ON p.user_id = u.id
                WHERE lower(u.email) = lower(?)
                """.trimIndent(),
            ).use { statement ->
                statement.setObject(1, email)
                statement.executeQuery().use { rs -> if (rs.next()) rs.toUserRecord() else null }
            }
        }

    fun findByVerificationToken(token: String): UserRecord? =
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                SELECT ${userColumns()}
                FROM users u
                LEFT JOIN profiles p ON p.user_id = u.id
                WHERE u.verification_token = ?
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, token)
                statement.executeQuery().use { rs -> if (rs.next()) rs.toUserRecord() else null }
            }
        }

    fun create(
        connection: Connection,
        userId: UUID,
        profileId: UUID,
        subscriptionId: UUID,
        email: String,
        firstName: String,
        lastName: String,
        passwordHash: String,
        verificationToken: String,
        rcCustomerId: String,
    ): UserRecord {
        connection.prepareStatement(
            """
            INSERT INTO users(id, email, password_hash, verification_token)
            VALUES (?, ?, ?, ?)
            """.trimIndent(),
        ).use { statement ->
            statement.setObject(1, userId)
            statement.setString(2, email)
            statement.setString(3, passwordHash)
            statement.setString(4, verificationToken)
            statement.executeUpdate()
        }

        connection.prepareStatement(
            """
            INSERT INTO profiles(id, user_id, first_name, last_name)
            VALUES (?, ?, ?, ?)
            """.trimIndent(),
        ).use { statement ->
            statement.setObject(1, profileId)
            statement.setObject(2, userId)
            statement.setString(3, firstName)
            statement.setString(4, lastName)
            statement.executeUpdate()
        }

        connection.prepareStatement(
            """
            INSERT INTO subscriptions(id, user_id, rc_customer_id, plan, status)
            VALUES (?, ?, ?, 'free', 'active')
            """.trimIndent(),
        ).use { statement ->
            statement.setObject(1, subscriptionId)
            statement.setObject(2, userId)
            statement.setString(3, rcCustomerId)
            statement.executeUpdate()
        }

        return findById(connection, userId) ?: error("Created user could not be loaded")
    }

    fun updateProfile(userId: UUID, firstName: String, lastName: String): UserRecord? =
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
            UPDATE profiles
            SET first_name = ?, last_name = ?
            WHERE user_id = ?
            """.trimIndent(),
            ).use { statement ->
                statement.setString(1, firstName)
                statement.setString(2, lastName)
                statement.setObject(3, userId)
                statement.executeUpdate()
            }
            findById(connection, userId)
        }

    fun saveRefreshTokenHash(userId: UUID, tokenHash: String) {
        dataSource.connection.use { connection ->
            saveRefreshTokenHash(connection, userId, tokenHash)
        }
    }

    fun clearRefreshTokenHash(userId: UUID) {
        dataSource.connection.use { connection ->
            clearRefreshTokenHash(connection, userId)
        }
    }

    fun verifyEmail(token: String): Boolean =
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                UPDATE users
                SET email_verified = TRUE, verification_token = NULL
                WHERE verification_token = ?
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, token)
                statement.executeUpdate() > 0
            }
        }

    fun updateVerificationToken(userId: UUID, token: String) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                UPDATE users
                SET verification_token = ?, email_verified = FALSE
                WHERE id = ?
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, token)
                statement.setObject(2, userId)
                statement.executeUpdate()
            }
        }
    }

    fun upsertPasswordResetToken(userId: UUID, token: String, expiresAt: Instant) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                INSERT INTO password_reset_tokens(user_id, token, expires_at)
                VALUES (?, ?, ?)
                ON CONFLICT (user_id)
                DO UPDATE SET token = EXCLUDED.token, expires_at = EXCLUDED.expires_at
                """.trimIndent(),
            ).use { statement ->
                statement.setObject(1, userId)
                statement.setString(2, token)
                statement.setObject(3, OffsetDateTime.ofInstant(expiresAt, ZoneOffset.UTC))
                statement.executeUpdate()
            }
        }
    }

    fun findPasswordResetToken(token: String): PasswordResetTokenRecord? =
        dataSource.connection.use { connection ->
            findPasswordResetToken(connection, token)
        }

    fun consumePasswordResetToken(token: String) {
        dataSource.connection.use { connection ->
            consumePasswordResetToken(connection, token)
        }
    }

    fun updatePassword(userId: UUID, newPasswordHash: String) {
        dataSource.connection.use { connection ->
            updatePassword(connection, userId, newPasswordHash)
        }
    }

    fun deleteById(userId: UUID): Boolean =
        dataSource.connection.use { connection ->
            connection.prepareStatement("DELETE FROM users WHERE id = ?").use { statement ->
                statement.setObject(1, userId)
                statement.executeUpdate() > 0
            }
        }

    fun findById(connection: Connection, userId: UUID): UserRecord? =
        connection.prepareStatement(
            """
            SELECT ${userColumns()}
            FROM users u
            LEFT JOIN profiles p ON p.user_id = u.id
            WHERE u.id = ?
            """.trimIndent(),
        ).use { statement ->
            statement.setObject(1, userId)
            statement.executeQuery().use { rs -> if (rs.next()) rs.toUserRecord() else null }
        }

    fun findPasswordResetToken(connection: Connection, token: String): PasswordResetTokenRecord? =
        connection.prepareStatement(
            """
            SELECT user_id, token, expires_at
            FROM password_reset_tokens
            WHERE token = ?
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, token)
            statement.executeQuery().use { rs ->
                if (rs.next()) {
                    PasswordResetTokenRecord(
                        userId = rs.getObject("user_id", UUID::class.java),
                        token = rs.getString("token"),
                        expiresAt = rs.getObject("expires_at", OffsetDateTime::class.java).toInstant(),
                    )
                } else {
                    null
                }
            }
        }

    fun consumePasswordResetToken(connection: Connection, token: String) {
        connection.prepareStatement("DELETE FROM password_reset_tokens WHERE token = ?").use { statement ->
            statement.setString(1, token)
            statement.executeUpdate()
        }
    }

    fun updatePassword(connection: Connection, userId: UUID, newPasswordHash: String) {
        connection.prepareStatement(
            """
            UPDATE users
            SET password_hash = ?, refresh_token_hash = NULL
            WHERE id = ?
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, newPasswordHash)
            statement.setObject(2, userId)
            statement.executeUpdate()
        }
    }

    fun clearRefreshTokenHash(connection: Connection, userId: UUID) {
        connection.prepareStatement("UPDATE users SET refresh_token_hash = NULL WHERE id = ?").use { statement ->
            statement.setObject(1, userId)
            statement.executeUpdate()
        }
    }

    private fun saveRefreshTokenHash(connection: Connection, userId: UUID, tokenHash: String) {
        connection.prepareStatement("UPDATE users SET refresh_token_hash = ? WHERE id = ?").use { statement ->
            statement.setString(1, tokenHash)
            statement.setObject(2, userId)
            statement.executeUpdate()
        }
    }

    private fun ResultSet.toUserRecord(): UserRecord =
        UserRecord(
            id = getObject("id", UUID::class.java),
            email = getString("email"),
            passwordHash = getString("password_hash"),
            emailVerified = getBoolean("email_verified"),
            verificationToken = getString("verification_token"),
            refreshTokenHash = getString("refresh_token_hash"),
            profileId = getObject("profile_id", UUID::class.java),
            firstName = getString("first_name"),
            lastName = getString("last_name"),
            avatarUrl = getString("avatar_url"),
        )

    private fun userColumns(): String =
        """
        u.id,
        u.email,
        u.password_hash,
        u.email_verified,
        u.verification_token,
        u.refresh_token_hash,
        p.id AS profile_id,
        p.first_name,
        p.last_name,
        p.avatar_url
        """.trimIndent()
}
