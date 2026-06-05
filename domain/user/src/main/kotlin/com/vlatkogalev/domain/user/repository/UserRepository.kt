package com.vlatkogalev.domain.user.repository

import com.vlatkogalev.domain.user.model.PasswordResetConfirmationResult
import com.vlatkogalev.domain.user.model.PasswordResetToken
import com.vlatkogalev.domain.user.model.UpgradeAnonymousResult
import com.vlatkogalev.domain.user.model.UserAuthIdentity
import com.vlatkogalev.domain.user.model.UserAccount
import java.time.Instant
import java.util.*

interface UserRepository {
    suspend fun findById(userId: UUID): UserAccount?

    suspend fun findByVerificationToken(token: String): UserAccount?

    suspend fun findUserByInstallationId(installationId: String): UserAccount?

    suspend fun create(
        email: String,
        firstName: String,
        lastName: String,
        passwordHash: String,
        verificationToken: String,
    ): UserAccount

    suspend fun updateProfile(userId: UUID, firstName: String, lastName: String): UserAccount?

    suspend fun createAnonymousInstallation(installationId: String, userId: UUID)

    suspend fun updateLastSeen(installationId: String)

    suspend fun createAuthIdentity(identity: UserAuthIdentity)

    suspend fun findAuthIdentityByEmail(email: String): UserAuthIdentity?

    suspend fun findAuthIdentitiesByUserId(userId: UUID): List<UserAuthIdentity>

    suspend fun createAnonymousUser(installationId: String): UserAccount

    suspend fun upgradeAnonymousUser(
        userId: UUID,
        email: String,
        passwordHash: String,
        verificationToken: String,
        markVerified: Boolean,
    ): UpgradeAnonymousResult

    suspend fun saveRefreshTokenHash(userId: UUID, tokenHash: String)

    suspend fun clearRefreshTokenHash(userId: UUID)

    suspend fun verifyEmail(token: String): Boolean

    suspend fun updateVerificationToken(userId: UUID, token: String)

    suspend fun upsertPasswordResetToken(userId: UUID, token: String, expiresAt: Instant)

    suspend fun findPasswordResetToken(token: String): PasswordResetToken?

    suspend fun consumePasswordResetToken(token: String)

    suspend fun updatePassword(userId: UUID, newPasswordHash: String)

    suspend fun confirmPasswordReset(token: String, newPasswordHash: String): PasswordResetConfirmationResult

    suspend fun deleteById(userId: UUID): Boolean
}
