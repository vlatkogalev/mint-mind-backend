package com.vlatkogalev.data.postgres.repository

import com.vlatkogalev.data.postgres.daos.UserQueries
import com.vlatkogalev.data.postgres.entities.PasswordResetTokenRecord
import com.vlatkogalev.data.postgres.entities.UserAuthIdentityRecord
import com.vlatkogalev.data.postgres.entities.UserRecord
import com.vlatkogalev.domain.user.model.AuthType
import com.vlatkogalev.domain.user.model.PasswordResetConfirmationResult
import com.vlatkogalev.domain.user.model.PasswordResetToken
import com.vlatkogalev.domain.user.model.UserAuthIdentity
import com.vlatkogalev.domain.user.model.UserAccount
import com.vlatkogalev.domain.user.model.UserProfile
import com.vlatkogalev.domain.user.repository.UserRepository
import com.vlatkogalev.platform.database.withTransaction
import java.time.Instant
import java.util.*
import javax.sql.DataSource

class UserRepositoryImpl(
    private val queries: UserQueries,
    private val dataSource: DataSource,
) : UserRepository {
    override fun findById(userId: UUID): UserAccount? = queries.findById(userId)?.toUserAccount()

    override fun findByVerificationToken(token: String): UserAccount? = queries.findByVerificationToken(token)?.toUserAccount()

    override fun findUserByInstallationId(installationId: String): UserAccount? =
        queries.findByInstallationId(installationId)?.toUserAccount()

    override fun create(
        email: String,
        firstName: String,
        lastName: String,
        passwordHash: String,
        verificationToken: String,
    ): UserAccount =
        dataSource.withTransaction { connection ->
            val userId = UUID.randomUUID()
            queries.create(
                connection = connection,
                userId = userId,
                profileId = UUID.randomUUID(),
                subscriptionId = UUID.randomUUID(),
                email = email,
                firstName = firstName,
                lastName = lastName,
                passwordHash = passwordHash,
                verificationToken = verificationToken,
                rcCustomerId = userId.toString(),
            ).toUserAccount()
        }

    override fun updateProfile(userId: UUID, firstName: String, lastName: String): UserAccount? =
        queries.updateProfile(userId, firstName, lastName)?.toUserAccount()

    override fun createAnonymousInstallation(installationId: String, userId: UUID) {
        queries.createAnonymousInstallation(installationId, userId)
    }

    override fun updateLastSeen(installationId: String) {
        queries.updateLastSeen(installationId)
    }

    override fun createAuthIdentity(identity: UserAuthIdentity) {
        queries.createAuthIdentity(
            id = identity.id,
            userId = identity.userId,
            authType = identity.authType.name,
            email = identity.email,
            passwordHash = identity.passwordHash,
        )
    }

    override fun findAuthIdentityByEmail(email: String): UserAuthIdentity? =
        queries.findAuthIdentityByEmail(email)?.toDomain()

    override fun findAuthIdentitiesByUserId(userId: UUID): List<UserAuthIdentity> =
        queries.findAuthIdentitiesByUserId(userId).map { it.toDomain() }

    override fun createAnonymousUser(installationId: String): UserAccount =
        dataSource.withTransaction { connection ->
            val userId = UUID.randomUUID()
            val user = queries.createAnonymousUser(
                connection = connection,
                userId = userId,
                profileId = UUID.randomUUID(),
                subscriptionId = UUID.randomUUID(),
                rcCustomerId = userId.toString(),
            )
            queries.createAuthIdentity(
                connection = connection,
                id = UUID.randomUUID(),
                userId = userId,
                authType = AuthType.ANONYMOUS.name,
                email = null,
                passwordHash = null,
            )
            queries.createAnonymousInstallation(installationId, userId)
            user.toUserAccount()
        }

    override fun upgradeAnonymousUser(
        userId: UUID,
        email: String,
        passwordHash: String,
        verificationToken: String,
        markVerified: Boolean,
    ): UserAccount? =
        dataSource.withTransaction { connection ->
            val upgraded = queries.upgradeAnonymousUser(
                connection = connection,
                userId = userId,
                email = email,
                passwordHash = passwordHash,
                verificationToken = verificationToken,
                markVerified = markVerified,
            )
            if (!upgraded) {
                return@withTransaction null
            }
            queries.createAuthIdentity(
                connection = connection,
                id = UUID.randomUUID(),
                userId = userId,
                authType = AuthType.EMAIL.name,
                email = email,
                passwordHash = passwordHash,
            )
            queries.findById(connection, userId)?.toUserAccount()
        }

    override fun saveRefreshTokenHash(userId: UUID, tokenHash: String) {
        queries.saveRefreshTokenHash(userId, tokenHash)
    }

    override fun clearRefreshTokenHash(userId: UUID) {
        queries.clearRefreshTokenHash(userId)
    }

    override fun verifyEmail(token: String): Boolean = queries.verifyEmail(token)

    override fun updateVerificationToken(userId: UUID, token: String) {
        queries.updateVerificationToken(userId, token)
    }

    override fun upsertPasswordResetToken(userId: UUID, token: String, expiresAt: Instant) {
        queries.upsertPasswordResetToken(userId, token, expiresAt)
    }

    override fun findPasswordResetToken(token: String): PasswordResetToken? =
        queries.findPasswordResetToken(token)?.toPasswordResetToken()

    override fun consumePasswordResetToken(token: String) {
        queries.consumePasswordResetToken(token)
    }

    override fun updatePassword(userId: UUID, newPasswordHash: String) {
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
            queries.clearRefreshTokenHash(connection, resetToken.userId)
            PasswordResetConfirmationResult.SUCCESS
        }

    override fun deleteById(userId: UUID): Boolean = queries.deleteById(userId)

    private fun UserRecord.toUserAccount(): UserAccount =
        UserAccount(
            id = id,
            email = email,
            passwordHash = passwordHash,
            emailVerified = emailVerified,
            verificationToken = verificationToken,
            verificationEmailSentAt = verificationEmailSentAt,
            refreshTokenHash = refreshTokenHash,
            isAnonymous = isAnonymous,
            upgradedAt = upgradedAt,
            profile = profileId?.let { id ->
                UserProfile(
                    id = id,
                    userId = this.id,
                    firstName = firstName.orEmpty(),
                    lastName = lastName.orEmpty(),
                    avatarUrl = avatarUrl,
                )
            },
        )

    private fun PasswordResetTokenRecord.toPasswordResetToken(): PasswordResetToken =
        PasswordResetToken(
            userId = userId,
            token = token,
            expiresAt = expiresAt,
        )

    private fun UserAuthIdentityRecord.toDomain(): UserAuthIdentity =
        UserAuthIdentity(
            id = id,
            userId = userId,
            authType = AuthType.valueOf(authType),
            email = email,
            passwordHash = passwordHash,
            createdAt = createdAt,
        )
}
