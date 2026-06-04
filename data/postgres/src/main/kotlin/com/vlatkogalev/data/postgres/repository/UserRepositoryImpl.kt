package com.vlatkogalev.data.postgres.repository

import com.vlatkogalev.domain.user.model.AuthType
import com.vlatkogalev.domain.user.model.PasswordResetConfirmationResult
import com.vlatkogalev.domain.user.model.PasswordResetToken
import com.vlatkogalev.domain.user.model.UpgradeAnonymousResult
import com.vlatkogalev.domain.user.model.UserAuthIdentity
import com.vlatkogalev.domain.user.model.UserAccount
import com.vlatkogalev.domain.user.model.UserProfile
import com.vlatkogalev.domain.user.repository.UserRepository
import com.vlatkogalev.platform.database.tables.AnonymousInstallationsTable
import com.vlatkogalev.platform.database.tables.PasswordResetTokensTable
import com.vlatkogalev.platform.database.tables.ProfilesTable
import com.vlatkogalev.platform.database.tables.SubscriptionsTable
import com.vlatkogalev.platform.database.tables.UserAuthIdentitiesTable
import com.vlatkogalev.platform.database.tables.UsersTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.upsert
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class UserRepositoryImpl : UserRepository {

    override suspend fun findById(userId: UUID): UserAccount? =
        newSuspendedTransaction { findUserById(userId) }

    override suspend fun findByVerificationToken(token: String): UserAccount? =
        newSuspendedTransaction {
            UsersTable.selectAll()
                .where { UsersTable.verificationToken eq token }
                .singleOrNull()
                ?.let { resultToUserAccount(it) }
        }

    override suspend fun findUserByInstallationId(installationId: String): UserAccount? =
        newSuspendedTransaction {
            AnonymousInstallationsTable.selectAll()
                .where { AnonymousInstallationsTable.installationId eq installationId }
                .singleOrNull()
                ?.let { aiRow ->
                    UsersTable.selectAll()
                        .where { UsersTable.id eq aiRow[AnonymousInstallationsTable.userId] }
                        .singleOrNull()
                        ?.let { userRow -> resultToUserAccount(userRow) }
                }
        }

    override suspend fun create(
        email: String,
        firstName: String,
        lastName: String,
        passwordHash: String,
        verificationToken: String,
    ): UserAccount =
        newSuspendedTransaction {
            val userId = UUID.randomUUID()
            val profileId = UUID.randomUUID()
            val subscriptionId = UUID.randomUUID()

            UsersTable.insert { stmt ->
                stmt[id] = userId
                stmt[UsersTable.email] = email
                stmt[UsersTable.passwordHash] = passwordHash
                stmt[UsersTable.verificationToken] = verificationToken
                stmt[UsersTable.isAnonymous] = false
            }

            ProfilesTable.insert { stmt ->
                stmt[id] = profileId
                stmt[ProfilesTable.userId] = userId
                stmt[ProfilesTable.firstName] = firstName
                stmt[ProfilesTable.lastName] = lastName
            }

            SubscriptionsTable.insert { stmt ->
                stmt[id] = subscriptionId
                stmt[SubscriptionsTable.userId] = userId
                stmt[SubscriptionsTable.rcCustomerId] = userId.toString()
                stmt[SubscriptionsTable.plan] = "free"
                stmt[SubscriptionsTable.status] = "active"
            }

            findUserById(userId) ?: error("Created user could not be loaded")
        }

    override suspend fun updateProfile(userId: UUID, firstName: String, lastName: String): UserAccount? =
        newSuspendedTransaction {
            ProfilesTable.update(
                where = { ProfilesTable.userId eq userId },
                body = {
                    it[ProfilesTable.firstName] = firstName
                    it[ProfilesTable.lastName] = lastName
                },
            )
            findUserById(userId)
        }

    override suspend fun createAnonymousInstallation(installationId: String, userId: UUID) {
        newSuspendedTransaction {
            AnonymousInstallationsTable.upsert(AnonymousInstallationsTable.installationId) { stmt ->
                stmt[AnonymousInstallationsTable.installationId] = installationId
                stmt[AnonymousInstallationsTable.userId] = userId
                stmt[AnonymousInstallationsTable.lastSeenAt] = nowUtc()
            }
        }
    }

    override suspend fun updateLastSeen(installationId: String) {
        newSuspendedTransaction {
            AnonymousInstallationsTable.update(
                where = { AnonymousInstallationsTable.installationId eq installationId },
                body = { it[AnonymousInstallationsTable.lastSeenAt] = nowUtc() },
            )
        }
    }

    override suspend fun createAuthIdentity(identity: UserAuthIdentity) {
        newSuspendedTransaction {
            UserAuthIdentitiesTable.insert { stmt ->
                stmt[id] = identity.id
                stmt[userId] = identity.userId
                stmt[authType] = identity.authType.name
                stmt[email] = identity.email
                stmt[passwordHash] = identity.passwordHash
            }
        }
    }

    override suspend fun findAuthIdentityByEmail(email: String): UserAuthIdentity? =
        newSuspendedTransaction {
            UserAuthIdentitiesTable.selectAll()
                .where { UserAuthIdentitiesTable.email eq email.lowercase() }
                .singleOrNull()
                ?.toUserAuthIdentity()
        }

    override suspend fun findAuthIdentitiesByUserId(userId: UUID): List<UserAuthIdentity> =
        newSuspendedTransaction {
            UserAuthIdentitiesTable.selectAll()
                .where { UserAuthIdentitiesTable.userId eq userId }
                .orderBy(UserAuthIdentitiesTable.createdAt to SortOrder.ASC)
                .map { it.toUserAuthIdentity() }
        }

    override suspend fun createAnonymousUser(installationId: String): UserAccount =
        newSuspendedTransaction {
            val userId = UUID.randomUUID()
            val profileId = UUID.randomUUID()
            val subscriptionId = UUID.randomUUID()

            UsersTable.insert { stmt ->
                stmt[id] = userId
                stmt[isAnonymous] = true
            }

            ProfilesTable.insert { stmt ->
                stmt[id] = profileId
                stmt[ProfilesTable.userId] = userId
                stmt[ProfilesTable.firstName] = "Anonymous"
                stmt[ProfilesTable.lastName] = "User"
            }

            SubscriptionsTable.insert { stmt ->
                stmt[id] = subscriptionId
                stmt[SubscriptionsTable.userId] = userId
                stmt[SubscriptionsTable.rcCustomerId] = userId.toString()
                stmt[SubscriptionsTable.plan] = "free"
                stmt[SubscriptionsTable.status] = "active"
            }

            UserAuthIdentitiesTable.insert { stmt ->
                stmt[id] = UUID.randomUUID()
                stmt[UserAuthIdentitiesTable.userId] = userId
                stmt[authType] = AuthType.ANONYMOUS.name
                stmt[email] = null
                stmt[passwordHash] = null
            }

            AnonymousInstallationsTable.insert { stmt ->
                stmt[AnonymousInstallationsTable.installationId] = installationId
                stmt[AnonymousInstallationsTable.userId] = userId
            }

            findUserById(userId) ?: error("Created anonymous user could not be loaded")
        }

    override suspend fun upgradeAnonymousUser(
        userId: UUID,
        email: String,
        firstName: String,
        lastName: String,
        passwordHash: String,
        verificationToken: String,
        markVerified: Boolean,
    ): UpgradeAnonymousResult =
        newSuspendedTransaction {
            val isAnonymous = UsersTable.selectAll()
                .where { UsersTable.id eq userId }
                .singleOrNull()
                ?.get(UsersTable.isAnonymous)

            if (isAnonymous == null) return@newSuspendedTransaction UpgradeAnonymousResult.NotFound
            if (!isAnonymous) return@newSuspendedTransaction UpgradeAnonymousResult.NotAnonymous

            val updated = UsersTable.update(
                where = { (UsersTable.id eq userId) and (UsersTable.isAnonymous eq true) },
                body = {
                    it[UsersTable.email] = email
                    it[UsersTable.passwordHash] = passwordHash
                    it[UsersTable.emailVerified] = markVerified
                    it[UsersTable.verificationToken] = if (markVerified) null else verificationToken
                    it[UsersTable.isAnonymous] = false
                    val now = nowUtc()
                    it[UsersTable.upgradedAt] = now
                    it[UsersTable.updatedAt] = now
                },
            ) > 0
            if (!updated) return@newSuspendedTransaction UpgradeAnonymousResult.NotFound

            ProfilesTable.update(
                where = { ProfilesTable.userId eq userId },
                body = {
                    it[ProfilesTable.firstName] = firstName
                    it[ProfilesTable.lastName] = lastName
                },
            )

            AnonymousInstallationsTable.deleteWhere {
                AnonymousInstallationsTable.userId eq userId
            }

            UserAuthIdentitiesTable.insert { stmt ->
                stmt[id] = UUID.randomUUID()
                stmt[UserAuthIdentitiesTable.userId] = userId
                stmt[UserAuthIdentitiesTable.authType] = AuthType.EMAIL.name
                stmt[UserAuthIdentitiesTable.email] = email
                stmt[UserAuthIdentitiesTable.passwordHash] = passwordHash
            }

            UpgradeAnonymousResult.Success(
                findUserById(userId) ?: return@newSuspendedTransaction UpgradeAnonymousResult.NotFound,
            )
        }

    override suspend fun saveRefreshTokenHash(userId: UUID, tokenHash: String) {
        newSuspendedTransaction {
            UsersTable.update(
                where = { UsersTable.id eq userId },
                body = { it[UsersTable.refreshTokenHash] = tokenHash },
            )
        }
    }

    override suspend fun clearRefreshTokenHash(userId: UUID) {
        newSuspendedTransaction {
            UsersTable.update(
                where = { UsersTable.id eq userId },
                body = { it[UsersTable.refreshTokenHash] = null },
            )
        }
    }

    override suspend fun verifyEmail(token: String): Boolean =
        newSuspendedTransaction {
            UsersTable.update(
                where = { UsersTable.verificationToken eq token },
                body = {
                    it[emailVerified] = true
                    it[verificationToken] = null
                },
            ) > 0
        }

    override suspend fun updateVerificationToken(userId: UUID, token: String) {
        newSuspendedTransaction {
            UsersTable.update(
                where = { UsersTable.id eq userId },
                body = {
                    it[verificationToken] = token
                    it[emailVerified] = false
                    it[verificationEmailSentAt] = nowUtc()
                },
            )
        }
    }

    override suspend fun upsertPasswordResetToken(userId: UUID, token: String, expiresAt: Instant) {
        newSuspendedTransaction {
            PasswordResetTokensTable.upsert(PasswordResetTokensTable.userId) { stmt ->
                stmt[PasswordResetTokensTable.userId] = userId
                stmt[PasswordResetTokensTable.token] = token
                stmt[PasswordResetTokensTable.expiresAt] = OffsetDateTime.ofInstant(expiresAt, ZoneOffset.UTC)
            }
        }
    }

    override suspend fun findPasswordResetToken(token: String): PasswordResetToken? =
        newSuspendedTransaction {
            PasswordResetTokensTable.selectAll()
                .where { PasswordResetTokensTable.token eq token }
                .singleOrNull()
                ?.toPasswordResetToken()
        }

    override suspend fun consumePasswordResetToken(token: String) {
        newSuspendedTransaction {
            PasswordResetTokensTable.deleteWhere { PasswordResetTokensTable.token eq token }
        }
    }

    override suspend fun updatePassword(userId: UUID, newPasswordHash: String) {
        newSuspendedTransaction {
            UsersTable.update(
                where = { UsersTable.id eq userId },
                body = {
                    it[UsersTable.passwordHash] = newPasswordHash
                    it[UsersTable.refreshTokenHash] = null
                },
            )
        }
    }

    override suspend fun confirmPasswordReset(
        token: String,
        newPasswordHash: String,
    ): PasswordResetConfirmationResult =
        newSuspendedTransaction {
            val resetToken = PasswordResetTokensTable.selectAll()
                .where { PasswordResetTokensTable.token eq token }
                .singleOrNull()
                ?: return@newSuspendedTransaction PasswordResetConfirmationResult.INVALID_TOKEN

            val now = OffsetDateTime.now(ZoneOffset.UTC)
            if (resetToken[PasswordResetTokensTable.expiresAt].isBefore(now)) {
                PasswordResetTokensTable.deleteWhere { PasswordResetTokensTable.token eq token }
                return@newSuspendedTransaction PasswordResetConfirmationResult.EXPIRED_TOKEN
            }

            val userId = resetToken[PasswordResetTokensTable.userId]
            UsersTable.update(
                where = { UsersTable.id eq userId },
                body = {
                    it[UsersTable.passwordHash] = newPasswordHash
                    it[UsersTable.refreshTokenHash] = null
                },
            )
            PasswordResetTokensTable.deleteWhere { PasswordResetTokensTable.token eq token }
            PasswordResetConfirmationResult.SUCCESS
        }

    override suspend fun deleteById(userId: UUID): Boolean =
        newSuspendedTransaction {
            UsersTable.deleteWhere { UsersTable.id eq userId } > 0
        }

    private fun findUserById(userId: UUID): UserAccount? =
        UsersTable.selectAll()
            .where { UsersTable.id eq userId }
            .singleOrNull()
            ?.let { resultToUserAccount(it) }

    private fun resultToUserAccount(userRow: ResultRow): UserAccount {
        val userId = userRow[UsersTable.id]
        val profile = ProfilesTable.selectAll()
            .where { ProfilesTable.userId eq userId }
            .singleOrNull()
        val profileId = profile?.get(ProfilesTable.id)

        return UserAccount(
            id = userId,
            email = userRow[UsersTable.email],
            passwordHash = userRow[UsersTable.passwordHash],
            emailVerified = userRow[UsersTable.emailVerified],
            verificationToken = userRow[UsersTable.verificationToken],
            verificationEmailSentAt = userRow[UsersTable.verificationEmailSentAt]?.toInstant(),
            refreshTokenHash = userRow[UsersTable.refreshTokenHash],
            isAnonymous = userRow[UsersTable.isAnonymous],
            upgradedAt = userRow[UsersTable.upgradedAt]?.toInstant(),
            profile = if (profile != null) UserProfile(
                id = profileId!!,
                userId = userId,
                firstName = profile[ProfilesTable.firstName],
                lastName = profile[ProfilesTable.lastName],
                avatarUrl = profile[ProfilesTable.avatarUrl],
            ) else null,
        )
    }

    private fun ResultRow.toPasswordResetToken(): PasswordResetToken =
        PasswordResetToken(
            userId = this[PasswordResetTokensTable.userId],
            token = this[PasswordResetTokensTable.token],
            expiresAt = this[PasswordResetTokensTable.expiresAt].toInstant(),
        )

    private fun ResultRow.toUserAuthIdentity(): UserAuthIdentity =
        UserAuthIdentity(
            id = this[UserAuthIdentitiesTable.id],
            userId = this[UserAuthIdentitiesTable.userId],
            authType = AuthType.valueOf(this[UserAuthIdentitiesTable.authType]),
            email = this[UserAuthIdentitiesTable.email],
            passwordHash = this[UserAuthIdentitiesTable.passwordHash],
            createdAt = this[UserAuthIdentitiesTable.createdAt].toInstant(),
        )

    private fun nowUtc(): OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC)
}
