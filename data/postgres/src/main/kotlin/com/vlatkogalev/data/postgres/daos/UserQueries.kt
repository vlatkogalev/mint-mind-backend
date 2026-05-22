package com.vlatkogalev.data.postgres.daos

import com.vlatkogalev.data.postgres.entities.PasswordResetTokenRecord
import com.vlatkogalev.data.postgres.entities.UserAuthIdentityRecord
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

    fun findByInstallationId(installationId: String): UserRecord? =
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                SELECT ${userColumns()}
                FROM anonymous_installations ai
                JOIN users u ON u.id = ai.user_id
                LEFT JOIN profiles p ON p.user_id = u.id
                WHERE ai.installation_id = ?
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, installationId)
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
            INSERT INTO users(id, email, password_hash, verification_token, is_anonymous)
            VALUES (?, ?, ?, ?, FALSE)
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

    fun createAnonymousUser(
        connection: Connection,
        userId: UUID,
        profileId: UUID,
        subscriptionId: UUID,
        rcCustomerId: String,
    ): UserRecord {
        connection.prepareStatement(
            """
            INSERT INTO users(id, is_anonymous)
            VALUES (?, TRUE)
            """.trimIndent(),
        ).use { statement ->
            statement.setObject(1, userId)
            statement.executeUpdate()
        }

        connection.prepareStatement(
            """
            INSERT INTO profiles(id, user_id, first_name, last_name)
            VALUES (?, ?, 'Anonymous', 'User')
            """.trimIndent(),
        ).use { statement ->
            statement.setObject(1, profileId)
            statement.setObject(2, userId)
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

        return findById(connection, userId) ?: error("Created anonymous user could not be loaded")
    }

    fun createAnonymousInstallation(installationId: String, userId: UUID) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                INSERT INTO anonymous_installations(installation_id, user_id)
                VALUES (?, ?)
                ON CONFLICT (installation_id)
                DO UPDATE SET user_id = EXCLUDED.user_id, last_seen_at = NOW()
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, installationId)
                statement.setObject(2, userId)
                statement.executeUpdate()
            }
        }
    }

    fun updateLastSeen(installationId: String) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                UPDATE anonymous_installations
                SET last_seen_at = NOW()
                WHERE installation_id = ?
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, installationId)
                statement.executeUpdate()
            }
        }
    }

    fun createAuthIdentity(
        id: UUID,
        userId: UUID,
        authType: String,
        email: String?,
        passwordHash: String?,
    ) {
        dataSource.connection.use { connection ->
            createAuthIdentity(connection, id, userId, authType, email, passwordHash)
        }
    }

    fun createAuthIdentity(
        connection: Connection,
        id: UUID,
        userId: UUID,
        authType: String,
        email: String?,
        passwordHash: String?,
    ) {
        connection.prepareStatement(
            """
            INSERT INTO user_auth_identities(id, user_id, auth_type, email, password_hash)
            VALUES (?, ?, ?, ?, ?)
            """.trimIndent(),
        ).use { statement ->
            statement.setObject(1, id)
            statement.setObject(2, userId)
            statement.setString(3, authType)
            statement.setString(4, email)
            statement.setString(5, passwordHash)
            statement.executeUpdate()
        }
    }

    fun findAuthIdentityByEmail(email: String): UserAuthIdentityRecord? =
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                SELECT id, user_id, auth_type, email, password_hash, created_at
                FROM user_auth_identities
                WHERE lower(email) = lower(?)
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, email)
                statement.executeQuery().use { rs ->
                    if (rs.next()) rs.toUserAuthIdentityRecord() else null
                }
            }
        }

    fun findAuthIdentitiesByUserId(userId: UUID): List<UserAuthIdentityRecord> =
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                SELECT id, user_id, auth_type, email, password_hash, created_at
                FROM user_auth_identities
                WHERE user_id = ?
                ORDER BY created_at ASC
                """.trimIndent(),
            ).use { statement ->
                statement.setObject(1, userId)
                statement.executeQuery().use { rs ->
                    buildList {
                        while (rs.next()) {
                            add(rs.toUserAuthIdentityRecord())
                        }
                    }
                }
            }
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
                SET verification_token = ?, email_verified = FALSE, verification_email_sent_at = NOW()
                WHERE id = ?
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, token)
                statement.setObject(2, userId)
                statement.executeUpdate()
            }
        }
    }

    fun upgradeAnonymousUser(
        connection: Connection,
        userId: UUID,
        email: String,
        passwordHash: String,
        verificationToken: String,
        markVerified: Boolean,
    ): Boolean {
        connection.prepareStatement(
            """
            UPDATE users
            SET
                email = ?,
                password_hash = ?,
                email_verified = ?,
                verification_token = CASE WHEN ? THEN NULL ELSE ? END,
                verification_email_sent_at = CASE WHEN ? THEN verification_email_sent_at ELSE NOW() END,
                is_anonymous = FALSE,
                upgraded_at = COALESCE(upgraded_at, NOW())
            WHERE id = ? AND is_anonymous = TRUE
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, email)
            statement.setString(2, passwordHash)
            statement.setBoolean(3, markVerified)
            statement.setBoolean(4, markVerified)
            statement.setString(5, verificationToken)
            statement.setBoolean(6, markVerified)
            statement.setObject(7, userId)
            return statement.executeUpdate() > 0
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
            verificationEmailSentAt = getTimestamp("verification_email_sent_at")?.toInstant(),
            refreshTokenHash = getString("refresh_token_hash"),
            profileId = getObject("profile_id", UUID::class.java),
            firstName = getString("first_name"),
            lastName = getString("last_name"),
            avatarUrl = getString("avatar_url"),
            isAnonymous = getBoolean("is_anonymous"),
            upgradedAt = getTimestamp("upgraded_at")?.toInstant(),
        )

    private fun ResultSet.toUserAuthIdentityRecord(): UserAuthIdentityRecord =
        UserAuthIdentityRecord(
            id = getObject("id", UUID::class.java),
            userId = getObject("user_id", UUID::class.java),
            authType = getString("auth_type"),
            email = getString("email"),
            passwordHash = getString("password_hash"),
            createdAt = getObject("created_at", OffsetDateTime::class.java).toInstant(),
        )

    private fun userColumns(): String =
        """
        u.id,
        u.email,
        u.password_hash,
        u.email_verified,
        u.verification_token,
        u.verification_email_sent_at,
        u.refresh_token_hash,
        u.is_anonymous,
        u.upgraded_at,
        p.id AS profile_id,
        p.first_name,
        p.last_name,
        p.avatar_url
        """.trimIndent()
}