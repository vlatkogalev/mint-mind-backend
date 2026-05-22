package com.vlatkogalev.domain.user.repository

import com.vlatkogalev.domain.user.model.PasswordResetConfirmationResult
import com.vlatkogalev.domain.user.model.PasswordResetToken
import com.vlatkogalev.domain.user.model.UpgradeAnonymousResult
import com.vlatkogalev.domain.user.model.UserAuthIdentity
import com.vlatkogalev.domain.user.model.UserAccount
import java.time.Instant
import java.util.*

interface UserRepository {
    fun findById(userId: UUID): UserAccount?

    fun findByVerificationToken(token: String): UserAccount?

    fun findUserByInstallationId(installationId: String): UserAccount?

    fun create(
        email: String,
        firstName: String,
        lastName: String,
        passwordHash: String,
        verificationToken: String,
    ): UserAccount

    fun updateProfile(userId: UUID, firstName: String, lastName: String): UserAccount?

    fun createAnonymousInstallation(installationId: String, userId: UUID)

    fun updateLastSeen(installationId: String)

    fun createAuthIdentity(identity: UserAuthIdentity)

    fun findAuthIdentityByEmail(email: String): UserAuthIdentity?

    fun findAuthIdentitiesByUserId(userId: UUID): List<UserAuthIdentity>

    fun createAnonymousUser(installationId: String): UserAccount

    fun upgradeAnonymousUser(
        userId: UUID,
        email: String,
        passwordHash: String,
        verificationToken: String,
        markVerified: Boolean,
    ): UpgradeAnonymousResult

    fun saveRefreshTokenHash(userId: UUID, tokenHash: String)

    fun clearRefreshTokenHash(userId: UUID)

    fun verifyEmail(token: String): Boolean

    fun updateVerificationToken(userId: UUID, token: String)

    fun upsertPasswordResetToken(userId: UUID, token: String, expiresAt: Instant)

    fun findPasswordResetToken(token: String): PasswordResetToken?

    fun consumePasswordResetToken(token: String)

    fun updatePassword(userId: UUID, newPasswordHash: String)

    fun confirmPasswordReset(token: String, newPasswordHash: String): PasswordResetConfirmationResult

    fun deleteById(userId: UUID): Boolean
}