package com.vlatkogalev.domain.user.service

import com.vlatkogalev.domain.user.model.PasswordResetConfirmationResult
import com.vlatkogalev.domain.user.model.PasswordResetToken
import com.vlatkogalev.domain.user.model.AuthType
import com.vlatkogalev.domain.user.model.UserAuthIdentity
import com.vlatkogalev.domain.user.model.UserAccount
import com.vlatkogalev.domain.user.model.UserProfile
import com.vlatkogalev.domain.user.model.UpgradeAnonymousResult
import com.vlatkogalev.domain.user.repository.UserRepository
import com.vlatkogalev.platform.core.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.security.MessageDigest
import java.time.Instant
import java.util.Base64
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.assertIs

class FakeUserRepository : UserRepository {
    private val users = mutableMapOf<UUID, UserAccount>()
    private val resetTokens = mutableMapOf<String, PasswordResetToken>()
    private val authIdentities = mutableMapOf<UUID, UserAuthIdentity>()
    private val installationToUserId = mutableMapOf<String, UUID>()

    var throwOnFindById = false
    var throwOnFindByEmail = false
    var throwOnCreate = false
    var throwOnUpdateProfile = false
    var throwOnSaveRefreshTokenHash = false
    var throwOnVerifyEmail = false
    var throwOnUpdateVerificationToken = false
    var throwOnUpsertPasswordResetToken = false
    var throwOnConfirmPasswordReset = false
    var throwOnDeleteById = false
    var throwOnFindPasswordResetToken = false
    var throwOnClearRefreshTokenHash = false
    var confirmPasswordResetCalls = 0
    var updateLastSeenCalls = 0

    fun reset() {
        users.clear()
        resetTokens.clear()
        authIdentities.clear()
        installationToUserId.clear()
        throwOnFindById = false
        throwOnFindByEmail = false
        throwOnCreate = false
        throwOnUpdateProfile = false
        throwOnSaveRefreshTokenHash = false
        throwOnVerifyEmail = false
        throwOnUpdateVerificationToken = false
        throwOnUpsertPasswordResetToken = false
        throwOnConfirmPasswordReset = false
        throwOnDeleteById = false
        throwOnFindPasswordResetToken = false
        throwOnClearRefreshTokenHash = false
        confirmPasswordResetCalls = 0
        updateLastSeenCalls = 0
    }

    suspend fun insert(user: UserAccount): UserAccount {
        users[user.id] = user
        if (!user.email.isNullOrBlank() && authIdentities.values.none { it.userId == user.id && it.authType == AuthType.EMAIL }) {
            createAuthIdentity(
                UserAuthIdentity(
                    id = UUID.randomUUID(),
                    userId = user.id,
                    authType = AuthType.EMAIL,
                    email = user.email,
                    passwordHash = user.passwordHash,
                    createdAt = Instant.now(),
                ),
            )
        }
        return user
    }

    fun findPasswordResetTokenByUserId(userId: UUID): PasswordResetToken? =
        resetTokens.values.firstOrNull { it.userId == userId }

    override suspend fun findById(userId: UUID): UserAccount? {
        if (throwOnFindById) error("findById failed")
        return users[userId]
    }

    override suspend fun findByVerificationToken(token: String): UserAccount? =
        users.values.firstOrNull { it.verificationToken == token }

    override suspend fun findUserByInstallationId(installationId: String): UserAccount? =
        installationToUserId[installationId]?.let { users[it] }

    override suspend fun create(
        email: String,
        firstName: String,
        lastName: String,
        passwordHash: String,
        verificationToken: String,
    ): UserAccount {
        if (throwOnCreate) error("create failed")
        val userId = UUID.randomUUID()
        return insert(
            UserAccount(
                id = userId,
                email = email,
                passwordHash = passwordHash,
                emailVerified = false,
                verificationToken = verificationToken,
                verificationEmailSentAt = null,
                refreshTokenHash = null,
                profile = UserProfile(
                    id = UUID.randomUUID(),
                    userId = userId,
                    firstName = firstName,
                    lastName = lastName,
                    avatarUrl = null,
                ),
                isAnonymous = false,
            ),
        )
    }

    override suspend fun updateProfile(userId: UUID, firstName: String, lastName: String): UserAccount? {
        if (throwOnUpdateProfile) error("updateProfile failed")
        val user = users[userId] ?: return null
        val currentProfile = user.profile ?: error("User profile is missing")
        val updated = user.copy(
            profile = currentProfile.copy(
                firstName = firstName,
                lastName = lastName,
            ),
        )
        users[userId] = updated
        return updated
    }

    override suspend fun createAnonymousInstallation(installationId: String, userId: UUID) {
        installationToUserId[installationId] = userId
    }

    override suspend fun updateLastSeen(installationId: String) {
        updateLastSeenCalls++
    }

    override suspend fun createAuthIdentity(identity: UserAuthIdentity) {
        authIdentities[identity.id] = identity
    }

    override suspend fun findAuthIdentityByEmail(email: String): UserAuthIdentity? =
        run {
            if (throwOnFindByEmail) error("findByEmail failed")
            authIdentities.values.firstOrNull { it.authType == AuthType.EMAIL && it.email.equals(email, ignoreCase = true) }
        }

    override suspend fun findAuthIdentitiesByUserId(userId: UUID): List<UserAuthIdentity> =
        authIdentities.values.filter { it.userId == userId }

    override suspend fun createAnonymousUser(installationId: String): UserAccount {
        if (throwOnCreate) error("create failed")
        val userId = UUID.randomUUID()
        val user = insert(
            UserAccount(
                id = userId,
                email = null,
                passwordHash = null,
                emailVerified = false,
                verificationToken = null,
                verificationEmailSentAt = null,
                refreshTokenHash = null,
                profile = UserProfile(
                    id = UUID.randomUUID(),
                    userId = userId,
                    firstName = "Anonymous",
                    lastName = "User",
                    avatarUrl = null,
                ),
                isAnonymous = true,
            ),
        )
        createAuthIdentity(
            UserAuthIdentity(
                id = UUID.randomUUID(),
                userId = userId,
                authType = AuthType.ANONYMOUS,
                email = null,
                passwordHash = null,
                createdAt = Instant.now(),
            ),
        )
        createAnonymousInstallation(installationId, userId)
        return user
    }

    override suspend fun upgradeAnonymousUser(
        userId: UUID,
        email: String,
        firstName: String,
        lastName: String,
        passwordHash: String,
        verificationToken: String,
        markVerified: Boolean,
    ): UpgradeAnonymousResult {
        val user = users[userId] ?: return UpgradeAnonymousResult.NotFound
        if (!user.isAnonymous) return UpgradeAnonymousResult.NotAnonymous
        val upgraded = user.copy(
            email = email,
            passwordHash = passwordHash,
            emailVerified = markVerified,
            verificationToken = if (markVerified) null else verificationToken,
            verificationEmailSentAt = if (markVerified) user.verificationEmailSentAt else Instant.now(),
            isAnonymous = false,
            upgradedAt = Instant.now(),
            profile = (user.profile ?: UserProfile(
                id = UUID.randomUUID(),
                userId = userId,
                firstName = firstName,
                lastName = lastName,
                avatarUrl = null,
            )).copy(firstName = firstName, lastName = lastName),
        )
        users[userId] = upgraded
        createAuthIdentity(
            UserAuthIdentity(
                id = UUID.randomUUID(),
                userId = userId,
                authType = AuthType.EMAIL,
                email = email,
                passwordHash = passwordHash,
                createdAt = Instant.now(),
            ),
        )
        return UpgradeAnonymousResult.Success(upgraded)
    }

    override suspend fun saveRefreshTokenHash(userId: UUID, tokenHash: String) {
        if (throwOnSaveRefreshTokenHash) error("saveRefreshTokenHash failed")
        val user = users[userId] ?: return
        users[userId] = user.copy(refreshTokenHash = tokenHash)
    }

    override suspend fun clearRefreshTokenHash(userId: UUID) {
        if (throwOnClearRefreshTokenHash) error("clearRefreshTokenHash failed")
        val user = users[userId] ?: return
        users[userId] = user.copy(refreshTokenHash = null)
    }

    override suspend fun verifyEmail(token: String): Boolean {
        if (throwOnVerifyEmail) error("verifyEmail failed")
        val user = findByVerificationToken(token) ?: return false
        users[user.id] = user.copy(
            emailVerified = true,
            verificationToken = null,
        )
        return true
    }

    override suspend fun updateVerificationToken(userId: UUID, token: String) {
        if (throwOnUpdateVerificationToken) error("updateVerificationToken failed")
        val user = users[userId] ?: return
        users[userId] = user.copy(
            verificationToken = token,
            verificationEmailSentAt = Instant.now(),
        )
    }

    override suspend fun upsertPasswordResetToken(userId: UUID, token: String, expiresAt: Instant) {
        if (throwOnUpsertPasswordResetToken) error("upsertPasswordResetToken failed")
        resetTokens.entries.removeIf { it.value.userId == userId }
        resetTokens[token] = PasswordResetToken(
            userId = userId,
            token = token,
            expiresAt = expiresAt,
        )
    }

    override suspend fun findPasswordResetToken(token: String): PasswordResetToken? {
        if (throwOnFindPasswordResetToken) error("findPasswordResetToken failed")
        return resetTokens[token]
    }

    override suspend fun consumePasswordResetToken(token: String) {
        resetTokens.remove(token)
    }

    override suspend fun updatePassword(userId: UUID, newPasswordHash: String) {
        val user = users[userId] ?: return
        users[userId] = user.copy(passwordHash = newPasswordHash)
        authIdentities.entries
            .filter { it.value.userId == userId && it.value.authType == AuthType.EMAIL }
            .forEach { (id, identity) ->
                authIdentities[id] = identity.copy(passwordHash = newPasswordHash)
            }
    }

    override suspend fun confirmPasswordReset(token: String, newPasswordHash: String): PasswordResetConfirmationResult {
        confirmPasswordResetCalls++
        if (throwOnConfirmPasswordReset) error("confirmPasswordReset failed")
        val resetToken = resetTokens[token] ?: return PasswordResetConfirmationResult.INVALID_TOKEN
        if (resetToken.expiresAt.isBefore(Instant.now())) {
            resetTokens.remove(token)
            return PasswordResetConfirmationResult.EXPIRED_TOKEN
        }
        updatePassword(resetToken.userId, newPasswordHash)
        consumePasswordResetToken(token)
        return PasswordResetConfirmationResult.SUCCESS
    }

    override suspend fun deleteById(userId: UUID): Boolean {
        if (throwOnDeleteById) error("deleteById failed")
        return users.remove(userId) != null
    }
}

class FakePasswordHasher : UserPasswordHasher {
    override suspend fun hash(password: String): String = "hashed:$password"
    override suspend fun verify(password: String, encodedHash: String): Boolean = encodedHash == "hashed:$password"
}

class FakeTokenProvider : UserTokenProvider {
    private val refreshCounts = mutableMapOf<UUID, Int>()

    override suspend fun createAccessToken(userId: UUID, isAnonymous: Boolean): String = "access:$userId"

    override suspend fun generateRefreshToken(userId: UUID): String {
        val nextCount = (refreshCounts[userId] ?: 0) + 1
        refreshCounts[userId] = nextCount
        return "$userId:refresh-token-$nextCount"
    }

    override suspend fun accessTokenExpiresInSeconds(): Long = 3600L
    override suspend fun refreshTokenExpiresInSeconds(): Long = 86400L
}

class RecordingEmailSender : EmailVerificationSender {
    val sentEmails = mutableListOf<Pair<String, String>>()

    override suspend fun sendVerificationEmail(email: String, verificationToken: String) {
        sentEmails += email to verificationToken
    }

    fun reset() = sentEmails.clear()
}

class RecordingPasswordResetEmailSender : PasswordResetEmailSender {
    val sentEmails = mutableListOf<Pair<String, String>>()

    override suspend fun sendPasswordResetEmail(email: String, resetToken: String) {
        sentEmails += email to resetToken
    }

    fun reset() = sentEmails.clear()
}

object TestFixtures {
    const val VALID_EMAIL = "test@example.com"
    const val VALID_PASSWORD = "Pass12345#"
    const val VALID_FIRST = "John"
    const val VALID_LAST = "Doe"

    fun makeAccount(
        id: UUID = UUID.randomUUID(),
        email: String = VALID_EMAIL,
        emailVerified: Boolean = false,
        verificationToken: String? = UUID.randomUUID().toString(),
        verificationEmailSentAt: Instant? = null,
        refreshTokenHash: String? = null,
        passwordHash: String = "hashed:$VALID_PASSWORD",
        profile: UserProfile? = UserProfile(
            id = UUID.randomUUID(),
            userId = id,
            firstName = VALID_FIRST,
            lastName = VALID_LAST,
            avatarUrl = null,
        ),
    ): UserAccount = UserAccount(
        id = id,
        email = email,
        passwordHash = passwordHash,
        emailVerified = emailVerified,
        verificationToken = verificationToken,
        verificationEmailSentAt = verificationEmailSentAt,
        refreshTokenHash = refreshTokenHash,
        profile = profile,
    )
}

abstract class UserAuthServiceTestBase {
    protected val repo = FakeUserRepository()
    protected val hasher = FakePasswordHasher()
    protected val tokenProvider = FakeTokenProvider()
    protected val emailSender = RecordingEmailSender()
    protected val passwordResetEmailSender = RecordingPasswordResetEmailSender()
    protected val skipVerificationDefault = false

    protected val testEmailScope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())

    protected lateinit var service: UserAuthServiceImpl

    @BeforeTest
    fun setup() {
        repo.reset()
        emailSender.reset()
        passwordResetEmailSender.reset()
        service = UserAuthServiceImpl(
            userRepository = repo,
            passwordHasher = hasher,
            jwtTokenProvider = tokenProvider,
            skipEmailVerification = skipVerificationDefault,
            emailVerificationSender = emailSender,
            passwordResetEmailSender = passwordResetEmailSender,
            emailScope = testEmailScope,
        )
    }

    protected suspend fun verifiedUser(email: String = TestFixtures.VALID_EMAIL): UserAccount = repo.insert(
        TestFixtures.makeAccount(
            email = email,
            emailVerified = true,
            verificationToken = null,
        ),
    )

    protected fun skipVerificationService(): UserAuthServiceImpl = UserAuthServiceImpl(
        userRepository = repo,
        passwordHasher = hasher,
        jwtTokenProvider = tokenProvider,
        skipEmailVerification = true,
        emailVerificationSender = emailSender,
        passwordResetEmailSender = passwordResetEmailSender,
        emailScope = testEmailScope,
    )

    protected fun hashToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(token.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(digest)
    }

    protected fun <T> assertSuccess(result: Result<T>): Result.Success<T> = assertIs<Result.Success<T>>(result)
    protected fun assertFailure(result: Result<*>): Result.Failure = assertIs<Result.Failure>(result)
}