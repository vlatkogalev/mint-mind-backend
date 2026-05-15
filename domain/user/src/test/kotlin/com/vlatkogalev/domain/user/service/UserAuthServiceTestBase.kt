package com.vlatkogalev.domain.user.service

import com.vlatkogalev.domain.user.model.PasswordResetConfirmationResult
import com.vlatkogalev.domain.user.model.PasswordResetToken
import com.vlatkogalev.domain.user.model.UserAccount
import com.vlatkogalev.domain.user.model.UserProfile
import com.vlatkogalev.domain.user.repository.UserRepository
import com.vlatkogalev.platform.core.Result
import java.security.MessageDigest
import java.time.Instant
import java.util.Base64
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.assertIs

class FakeUserRepository : UserRepository {
    private val users = mutableMapOf<UUID, UserAccount>()
    private val resetTokens = mutableMapOf<String, PasswordResetToken>()

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
    var confirmPasswordResetCalls = 0

    fun reset() {
        users.clear()
        resetTokens.clear()
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
        confirmPasswordResetCalls = 0
    }

    fun insert(user: UserAccount): UserAccount {
        users[user.id] = user
        return user
    }

    override fun findById(userId: UUID): UserAccount? {
        if (throwOnFindById) error("findById failed")
        return users[userId]
    }

    override fun findByEmail(email: String): UserAccount? {
        if (throwOnFindByEmail) error("findByEmail failed")
        return users.values.firstOrNull { it.email == email }
    }

    override fun findByVerificationToken(token: String): UserAccount? =
        users.values.firstOrNull { it.verificationToken == token }

    override fun create(
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
            ),
        )
    }

    override fun updateProfile(userId: UUID, firstName: String, lastName: String): UserAccount? {
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

    override fun saveRefreshTokenHash(userId: UUID, tokenHash: String) {
        if (throwOnSaveRefreshTokenHash) error("saveRefreshTokenHash failed")
        val user = users[userId] ?: return
        users[userId] = user.copy(refreshTokenHash = tokenHash)
    }

    override fun clearRefreshTokenHash(userId: UUID) {
        val user = users[userId] ?: return
        users[userId] = user.copy(refreshTokenHash = null)
    }

    override fun verifyEmail(token: String): Boolean {
        if (throwOnVerifyEmail) error("verifyEmail failed")
        val user = findByVerificationToken(token) ?: return false
        users[user.id] = user.copy(
            emailVerified = true,
            verificationToken = null,
        )
        return true
    }

    override fun updateVerificationToken(userId: UUID, token: String) {
        if (throwOnUpdateVerificationToken) error("updateVerificationToken failed")
        val user = users[userId] ?: return
        users[userId] = user.copy(
            verificationToken = token,
            verificationEmailSentAt = Instant.now(),
        )
    }

    override fun upsertPasswordResetToken(userId: UUID, token: String, expiresAt: Instant) {
        if (throwOnUpsertPasswordResetToken) error("upsertPasswordResetToken failed")
        resetTokens.entries.removeIf { it.value.userId == userId }
        resetTokens[token] = PasswordResetToken(
            userId = userId,
            token = token,
            expiresAt = expiresAt,
        )
    }

    override fun findPasswordResetToken(token: String): PasswordResetToken? = resetTokens[token]

    override fun consumePasswordResetToken(token: String) {
        resetTokens.remove(token)
    }

    override fun updatePassword(userId: UUID, newPasswordHash: String) {
        val user = users[userId] ?: return
        users[userId] = user.copy(passwordHash = newPasswordHash)
    }

    override fun confirmPasswordReset(token: String, newPasswordHash: String): PasswordResetConfirmationResult {
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

    override fun deleteById(userId: UUID): Boolean {
        if (throwOnDeleteById) error("deleteById failed")
        return users.remove(userId) != null
    }
}

class FakePasswordHasher : UserPasswordHasher {
    override fun hash(password: String): String = "hashed:$password"
    override fun verify(password: String, encodedHash: String): Boolean = encodedHash == "hashed:$password"
}

class FakeTokenProvider : UserTokenProvider {
    private val refreshCounts = mutableMapOf<UUID, Int>()

    override fun createAccessToken(userId: UUID, email: String): String = "access:$userId"

    override fun generateRefreshToken(userId: UUID): String {
        val nextCount = (refreshCounts[userId] ?: 0) + 1
        refreshCounts[userId] = nextCount
        return "$userId:refresh-token-$nextCount"
    }

    override fun accessTokenExpiresInSeconds(): Long = 3600L
    override fun refreshTokenExpiresInSeconds(): Long = 86400L
}

class RecordingEmailSender : EmailVerificationSender {
    val sentEmails = mutableListOf<Pair<String, String>>()

    override fun sendVerificationEmail(email: String, verificationToken: String) {
        sentEmails += email to verificationToken
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
    protected val skipVerificationDefault = false

    protected lateinit var service: UserAuthServiceImpl

    @BeforeTest
    fun setup() {
        repo.reset()
        emailSender.reset()
        service = UserAuthServiceImpl(repo, hasher, tokenProvider, skipVerificationDefault, emailSender)
    }

    protected fun verifiedUser(email: String = TestFixtures.VALID_EMAIL): UserAccount = repo.insert(
        TestFixtures.makeAccount(
            email = email,
            emailVerified = true,
            verificationToken = null,
        ),
    )

    protected fun skipVerificationService(): UserAuthServiceImpl = UserAuthServiceImpl(
        repo,
        hasher,
        tokenProvider,
        true,
        emailSender,
    )

    protected fun hashToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(token.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(digest)
    }

    protected fun <T> assertSuccess(result: Result<T>): Result.Success<T> = assertIs<Result.Success<T>>(result)
    protected fun assertFailure(result: Result<*>): Result.Failure = assertIs<Result.Failure>(result)
}
