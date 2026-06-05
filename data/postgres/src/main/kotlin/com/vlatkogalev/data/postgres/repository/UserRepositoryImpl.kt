package com.vlatkogalev.data.postgres.repository

import com.vlatkogalev.data.postgres.tables.AnonymousInstallationsTable
import com.vlatkogalev.data.postgres.tables.PasswordResetTokensTable
import com.vlatkogalev.data.postgres.tables.ProfilesTable
import com.vlatkogalev.data.postgres.tables.SubscriptionsTable
import com.vlatkogalev.data.postgres.tables.UserAuthIdentitiesTable
import com.vlatkogalev.data.postgres.tables.UsersTable
import com.vlatkogalev.domain.user.model.AuthType
import com.vlatkogalev.domain.user.model.PasswordResetConfirmationResult
import com.vlatkogalev.domain.user.model.PasswordResetToken
import com.vlatkogalev.domain.user.model.UpgradeAnonymousResult
import com.vlatkogalev.domain.user.model.UserAuthIdentity
import com.vlatkogalev.domain.user.model.UserAccount
import com.vlatkogalev.domain.user.model.UserProfile
import com.vlatkogalev.domain.user.repository.UserRepository
import com.vlatkogalev.platform.database.dbQuery
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.*
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*

class UserRepositoryImpl(
    private val database: R2dbcDatabase,
) : UserRepository {
    override suspend fun findById(userId: UUID): UserAccount? =
        dbQuery(database) {
            (UsersTable leftJoin ProfilesTable)
                .selectAll()
                .where { UsersTable.id eq userId }
                .firstOrNull()
                ?.toUserAccount()
        }

    override suspend fun findByVerificationToken(token: String): UserAccount? =
        dbQuery(database) {
            (UsersTable leftJoin ProfilesTable)
                .selectAll()
                .where { UsersTable.verificationToken eq token }
                .firstOrNull()
                ?.toUserAccount()
        }

    override suspend fun findUserByInstallationId(installationId: String): UserAccount? =
        dbQuery(database) {
            ((AnonymousInstallationsTable innerJoin UsersTable) leftJoin ProfilesTable)
                .selectAll()
                .where { AnonymousInstallationsTable.installationId eq installationId }
                .firstOrNull()
                ?.toUserAccount()
        }

    override suspend fun create(
        email: String,
        firstName: String,
        lastName: String,
        passwordHash: String,
        verificationToken: String,
    ): UserAccount =
        dbQuery(database) {
            val userId = UUID.randomUUID()

            UsersTable.insert {
                it[id] = userId
                it[UsersTable.email] = email
                it[UsersTable.passwordHash] = passwordHash
                it[UsersTable.verificationToken] = verificationToken
                it[UsersTable.isAnonymous] = false
            }

            ProfilesTable.insert {
                it[id] = UUID.randomUUID()
                it[ProfilesTable.userId] = userId
                it[ProfilesTable.firstName] = firstName
                it[ProfilesTable.lastName] = lastName
            }

            SubscriptionsTable.insert {
                it[id] = UUID.randomUUID()
                it[SubscriptionsTable.userId] = userId
                it[SubscriptionsTable.rcCustomerId] = userId.toString()
                it[SubscriptionsTable.plan] = "free"
                it[SubscriptionsTable.status] = "active"
            }

            (UsersTable leftJoin ProfilesTable)
                .selectAll()
                .where { UsersTable.id eq userId }
                .first()
                .toUserAccount()
        }

    override suspend fun updateProfile(userId: UUID, firstName: String, lastName: String): UserAccount? =
        dbQuery(database) {
            ProfilesTable.update({ ProfilesTable.userId eq userId }) {
                it[ProfilesTable.firstName] = firstName
                it[ProfilesTable.lastName] = lastName
            }

            (UsersTable leftJoin ProfilesTable)
                .selectAll()
                .where { UsersTable.id eq userId }
                .firstOrNull()
                ?.toUserAccount()
        }

    override suspend fun createAnonymousInstallation(installationId: String, userId: UUID): Unit =
        dbQuery(database) {
            val now = OffsetDateTime.now(ZoneOffset.UTC)
            AnonymousInstallationsTable.upsert(
                AnonymousInstallationsTable.installationId,
            ) {
                it[AnonymousInstallationsTable.installationId] = installationId
                it[AnonymousInstallationsTable.userId] = userId
                it[AnonymousInstallationsTable.lastSeenAt] = now
            }
        }

    override suspend fun updateLastSeen(installationId: String): Unit =
        dbQuery(database) {
            AnonymousInstallationsTable.update({ AnonymousInstallationsTable.installationId eq installationId }) {
                it[AnonymousInstallationsTable.lastSeenAt] = OffsetDateTime.now(ZoneOffset.UTC)
            }
        }

    override suspend fun createAuthIdentity(identity: UserAuthIdentity): Unit =
        dbQuery(database) {
            UserAuthIdentitiesTable.insert {
                it[UserAuthIdentitiesTable.id] = identity.id
                it[UserAuthIdentitiesTable.userId] = identity.userId
                it[UserAuthIdentitiesTable.authType] = identity.authType.name
                it[UserAuthIdentitiesTable.email] = identity.email
                it[UserAuthIdentitiesTable.passwordHash] = identity.passwordHash
                it[UserAuthIdentitiesTable.createdAt] = OffsetDateTime.ofInstant(identity.createdAt, ZoneOffset.UTC)
            }
        }

    override suspend fun findAuthIdentityByEmail(email: String): UserAuthIdentity? =
        dbQuery(database) {
            UserAuthIdentitiesTable
                .selectAll()
                .where { UserAuthIdentitiesTable.email.lowerCase() eq email.lowercase() }
                .firstOrNull()
                ?.toUserAuthIdentity()
        }

    override suspend fun findAuthIdentitiesByUserId(userId: UUID): List<UserAuthIdentity> =
        dbQuery(database) {
            UserAuthIdentitiesTable
                .selectAll()
                .where { UserAuthIdentitiesTable.userId eq userId }
                .orderBy(UserAuthIdentitiesTable.createdAt)
                .toList()
                .map { it.toUserAuthIdentity() }
        }

    override suspend fun createAnonymousUser(installationId: String): UserAccount =
        dbQuery(database) {
            val userId = UUID.randomUUID()

            UsersTable.insert {
                it[id] = userId
                it[UsersTable.isAnonymous] = true
            }

            ProfilesTable.insert {
                it[id] = UUID.randomUUID()
                it[ProfilesTable.userId] = userId
                it[ProfilesTable.firstName] = "Anonymous"
                it[ProfilesTable.lastName] = "User"
            }

            SubscriptionsTable.insert {
                it[id] = UUID.randomUUID()
                it[SubscriptionsTable.userId] = userId
                it[SubscriptionsTable.rcCustomerId] = userId.toString()
                it[SubscriptionsTable.plan] = "free"
                it[SubscriptionsTable.status] = "active"
            }

            UserAuthIdentitiesTable.insert {
                it[id] = UUID.randomUUID()
                it[UserAuthIdentitiesTable.userId] = userId
                it[UserAuthIdentitiesTable.authType] = AuthType.ANONYMOUS.name
                it[UserAuthIdentitiesTable.email] = null
                it[UserAuthIdentitiesTable.passwordHash] = null
                it[UserAuthIdentitiesTable.createdAt] = OffsetDateTime.now(ZoneOffset.UTC)
            }

            AnonymousInstallationsTable.insert {
                it[AnonymousInstallationsTable.installationId] = installationId
                it[AnonymousInstallationsTable.userId] = userId
                it[AnonymousInstallationsTable.lastSeenAt] = OffsetDateTime.now(ZoneOffset.UTC)
            }

            (UsersTable leftJoin ProfilesTable)
                .selectAll()
                .where { UsersTable.id eq userId }
                .first()
                .toUserAccount()
        }

    override suspend fun upgradeAnonymousUser(
        userId: UUID,
        email: String,
        passwordHash: String,
        verificationToken: String,
        markVerified: Boolean,
    ): UpgradeAnonymousResult =
        dbQuery(database) {
            val current = (UsersTable leftJoin ProfilesTable)
                .selectAll()
                .where { UsersTable.id eq userId }
                .firstOrNull()
                ?: return@dbQuery UpgradeAnonymousResult.NotFound

            if (current[UsersTable.isAnonymous] == false) {
                return@dbQuery UpgradeAnonymousResult.NotAnonymous
            }

            val now = OffsetDateTime.now(ZoneOffset.UTC)
            val updated = UsersTable.update({
                (UsersTable.id eq userId) and (UsersTable.isAnonymous eq true)
            }) {
                it[UsersTable.email] = email
                it[UsersTable.passwordHash] = passwordHash
                it[UsersTable.emailVerified] = markVerified
                it[UsersTable.verificationToken] = if (markVerified) null else verificationToken
                if (!markVerified) {
                    it[UsersTable.verificationEmailSentAt] = now
                }
                it[UsersTable.isAnonymous] = false
                it[UsersTable.upgradedAt] = now
            }

            if (updated == 0) return@dbQuery UpgradeAnonymousResult.NotFound

            AnonymousInstallationsTable.deleteWhere {
                AnonymousInstallationsTable.userId eq userId
            }

            UserAuthIdentitiesTable.insert {
                it[UserAuthIdentitiesTable.id] = UUID.randomUUID()
                it[UserAuthIdentitiesTable.userId] = userId
                it[UserAuthIdentitiesTable.authType] = AuthType.EMAIL.name
                it[UserAuthIdentitiesTable.email] = email
                it[UserAuthIdentitiesTable.passwordHash] = passwordHash
                it[UserAuthIdentitiesTable.createdAt] = OffsetDateTime.now(ZoneOffset.UTC)
            }

            val result = (UsersTable leftJoin ProfilesTable)
                .selectAll()
                .where { UsersTable.id eq userId }
                .firstOrNull()
                ?.toUserAccount()
                ?: return@dbQuery UpgradeAnonymousResult.NotFound

            UpgradeAnonymousResult.Success(result)
        }

    override suspend fun saveRefreshTokenHash(userId: UUID, tokenHash: String): Unit =
        dbQuery(database) {
            UsersTable.update({ UsersTable.id eq userId }) {
                it[UsersTable.refreshTokenHash] = tokenHash
            }
        }

    override suspend fun clearRefreshTokenHash(userId: UUID): Unit =
        dbQuery(database) {
            UsersTable.update({ UsersTable.id eq userId }) {
                it[UsersTable.refreshTokenHash] = null
            }
        }

    override suspend fun verifyEmail(token: String): Boolean =
        dbQuery(database) {
            UsersTable.update({ UsersTable.verificationToken eq token }) {
                it[UsersTable.emailVerified] = true
                it[UsersTable.verificationToken] = null
            } > 0
        }

    override suspend fun updateVerificationToken(userId: UUID, token: String): Unit =
        dbQuery(database) {
            val now = OffsetDateTime.now(ZoneOffset.UTC)
            UsersTable.update({ UsersTable.id eq userId }) {
                it[UsersTable.verificationToken] = token
                it[UsersTable.emailVerified] = false
                it[UsersTable.verificationEmailSentAt] = now
            }
        }

    override suspend fun upsertPasswordResetToken(userId: UUID, token: String, expiresAt: Instant): Unit =
        dbQuery(database) {
            PasswordResetTokensTable.upsert(PasswordResetTokensTable.userId) {
                it[PasswordResetTokensTable.userId] = userId
                it[PasswordResetTokensTable.token] = token
                it[PasswordResetTokensTable.expiresAt] = OffsetDateTime.ofInstant(expiresAt, ZoneOffset.UTC)
            }
        }

    override suspend fun findPasswordResetToken(token: String): PasswordResetToken? =
        dbQuery(database) {
            PasswordResetTokensTable
                .selectAll()
                .where { PasswordResetTokensTable.token eq token }
                .firstOrNull()
                ?.toPasswordResetToken()
        }

    override suspend fun consumePasswordResetToken(token: String): Unit =
        dbQuery(database) {
            PasswordResetTokensTable.deleteWhere { PasswordResetTokensTable.token eq token }
        }

    override suspend fun updatePassword(userId: UUID, newPasswordHash: String): Unit =
        dbQuery(database) {
            UsersTable.update({ UsersTable.id eq userId }) {
                it[UsersTable.passwordHash] = newPasswordHash
                it[UsersTable.refreshTokenHash] = null
            }
        }

    override suspend fun confirmPasswordReset(
        token: String,
        newPasswordHash: String,
    ): PasswordResetConfirmationResult =
        dbQuery(database) {
            val resetToken = PasswordResetTokensTable
                .selectAll()
                .where { PasswordResetTokensTable.token eq token }
                .firstOrNull()
                ?: return@dbQuery PasswordResetConfirmationResult.INVALID_TOKEN

            val expiresAt = resetToken[PasswordResetTokensTable.expiresAt].toInstant()
            if (expiresAt.isBefore(Instant.now())) {
                PasswordResetTokensTable.deleteWhere { PasswordResetTokensTable.token eq token }
                return@dbQuery PasswordResetConfirmationResult.EXPIRED_TOKEN
            }

            val userId = resetToken[PasswordResetTokensTable.userId]
            UsersTable.update({ UsersTable.id eq userId }) {
                it[UsersTable.passwordHash] = newPasswordHash
                it[UsersTable.refreshTokenHash] = null
            }
            PasswordResetTokensTable.deleteWhere { PasswordResetTokensTable.token eq token }

            PasswordResetConfirmationResult.SUCCESS
        }

    override suspend fun deleteById(userId: UUID): Boolean =
        dbQuery(database) {
            UsersTable.deleteWhere { UsersTable.id eq userId } > 0
        }

    private fun ResultRow.toUserAccount(): UserAccount {
        val profileId = getOrNull(ProfilesTable.id)
        return UserAccount(
            id = this[UsersTable.id],
            email = this[UsersTable.email],
            passwordHash = this[UsersTable.passwordHash],
            emailVerified = this[UsersTable.emailVerified],
            verificationToken = this[UsersTable.verificationToken],
            verificationEmailSentAt = this[UsersTable.verificationEmailSentAt]?.toInstant(),
            refreshTokenHash = this[UsersTable.refreshTokenHash],
            isAnonymous = this[UsersTable.isAnonymous],
            upgradedAt = this[UsersTable.upgradedAt]?.toInstant(),
            profile = if (profileId != null) {
                UserProfile(
                    id = profileId,
                    userId = this[UsersTable.id],
                    firstName = this[ProfilesTable.firstName],
                    lastName = this[ProfilesTable.lastName],
                    avatarUrl = this[ProfilesTable.avatarUrl],
                )
            } else null,
        )
    }

    private fun ResultRow.toUserAuthIdentity(): UserAuthIdentity =
        UserAuthIdentity(
            id = this[UserAuthIdentitiesTable.id],
            userId = this[UserAuthIdentitiesTable.userId],
            authType = AuthType.valueOf(this[UserAuthIdentitiesTable.authType]),
            email = this[UserAuthIdentitiesTable.email],
            passwordHash = this[UserAuthIdentitiesTable.passwordHash],
            createdAt = this[UserAuthIdentitiesTable.createdAt].toInstant(),
        )

    private fun ResultRow.toPasswordResetToken(): PasswordResetToken =
        PasswordResetToken(
            userId = this[PasswordResetTokensTable.userId],
            token = this[PasswordResetTokensTable.token],
            expiresAt = this[PasswordResetTokensTable.expiresAt].toInstant(),
        )
}
