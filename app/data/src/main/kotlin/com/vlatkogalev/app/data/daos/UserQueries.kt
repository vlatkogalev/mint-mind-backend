package com.vlatkogalev.app.data.daos

import com.vlatkogalev.app.data.entities.UserRecord
import com.vlatkogalev.app.data.entities .PasswordResetTokenRecord
import java.sql.Statement
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import javax.sql.DataSource

class UserQueries(
    private val dataSource: DataSource,
) {
    fun findById(userId: Long): UserRecord? =
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                SELECT id, email, full_name, password_hash
                FROM users
                WHERE id = ?
                """.trimIndent(),
            ).use { statement ->
                statement.setLong(1, userId)
                statement.executeQuery().use { rs ->
                    if (rs.next()) {
                        UserRecord(
                            id = rs.getLong("id"),
                            email = rs.getString("email"),
                            fullName = rs.getString("full_name"),
                            passwordHash = rs.getString("password_hash"),
                        )
                    } else {
                        null
                    }
                }
            }
        }

    fun findByEmail(email: String): UserRecord? =
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                SELECT id, email, full_name, password_hash
                FROM users
                WHERE lower(email) = lower(?)
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, email)
                statement.executeQuery().use { rs ->
                    if (rs.next()) {
                        UserRecord(
                            id = rs.getLong("id"),
                            email = rs.getString("email"),
                            fullName = rs.getString("full_name"),
                            passwordHash = rs.getString("password_hash"),
                        )
                    } else {
                        null
                    }
                }
            }
        }

    fun create(email: String, fullName: String, passwordHash: String): UserRecord =
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                INSERT INTO users(email, full_name, password_hash)
                VALUES (?, ?, ?)
                """.trimIndent(),
                Statement.RETURN_GENERATED_KEYS,
            ).use { statement ->
                statement.setString(1, email)
                statement.setString(2, fullName)
                statement.setString(3, passwordHash)
                statement.executeUpdate()

                statement.generatedKeys.use { keys ->
                    keys.next()
                    UserRecord(
                        id = keys.getLong(1),
                        email = email,
                        fullName = fullName,
                        passwordHash = passwordHash,
                    )
                }
            }
        }

    fun saveRefreshToken(userId: Long, token: String, expiresAt: Instant) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                INSERT INTO refresh_tokens(user_id, token, expires_at)
                VALUES (?, ?, ?)
                """.trimIndent(),
            ).use { statement ->
                statement.setLong(1, userId)
                statement.setString(2, token)
                statement.setObject(3, OffsetDateTime.ofInstant(expiresAt, ZoneOffset.UTC))
                statement.executeUpdate()
            }
        }
    }

    fun revokeRefreshTokensForUser(userId: Long) {
        dataSource.connection.use { connection ->
            connection.prepareStatement("DELETE FROM refresh_tokens WHERE user_id = ?").use { statement ->
                statement.setLong(1, userId)
                statement.executeUpdate()
            }
        }
    }

    fun upsertPasswordResetToken(userId: Long, token: String, expiresAt: Instant) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                INSERT INTO password_reset_tokens(user_id, token, expires_at)
                VALUES (?, ?, ?)
                ON CONFLICT (user_id)
                DO UPDATE SET token = EXCLUDED.token, expires_at = EXCLUDED.expires_at
                """.trimIndent(),
            ).use { statement ->
                statement.setLong(1, userId)
                statement.setString(2, token)
                statement.setObject(3, OffsetDateTime.ofInstant(expiresAt, ZoneOffset.UTC))
                statement.executeUpdate()
            }
        }
    }

    fun findPasswordResetToken(token: String): PasswordResetTokenRecord? =
        dataSource.connection.use { connection ->
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
                            userId = rs.getLong("user_id"),
                            token = rs.getString("token"),
                            expiresAt = rs.getObject("expires_at", OffsetDateTime::class.java).toInstant(),
                        )
                    } else {
                        null
                    }
                }
            }
        }

    fun consumePasswordResetToken(token: String) {
        dataSource.connection.use { connection ->
            connection.prepareStatement("DELETE FROM password_reset_tokens WHERE token = ?").use { statement ->
                statement.setString(1, token)
                statement.executeUpdate()
            }
        }
    }

    fun updatePassword(userId: Long, newPasswordHash: String) {
        dataSource.connection.use { connection ->
            connection.prepareStatement("UPDATE users SET password_hash = ?, updated_at = now() WHERE id = ?").use { statement ->
                statement.setString(1, newPasswordHash)
                statement.setLong(2, userId)
                statement.executeUpdate()
            }
        }
    }

    fun deleteById(userId: Long): Boolean =
        dataSource.connection.use { connection ->
            connection.prepareStatement("DELETE FROM users WHERE id = ?").use { statement ->
                statement.setLong(1, userId)
                statement.executeUpdate() > 0
            }
        }
}
