package com.vlatkogalev.domain.user.service

import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

class ConfirmPasswordResetTest : UserAuthServiceTestBase() {
    private suspend fun setupValidToken(): String {
        val user = verifiedUser()
        val token = "valid-token"
        repo.upsertPasswordResetToken(user.id, token, Instant.now().plus(15, ChronoUnit.MINUTES))
        return token
    }

    @Test
    fun confirmPasswordReset_withInvalidToken_returnsFailure() = runTest {
        val result = service.confirmPasswordReset("invalid-token", "NewPass1#")

        assertEquals("Invalid reset token", assertFailure(result).reason)
    }

    @Test
    fun confirmPasswordReset_withExpiredToken_returnsFailure() = runTest {
        val user = verifiedUser()
        repo.upsertPasswordResetToken(user.id, "expired-token", Instant.now().minus(1, ChronoUnit.MINUTES))

        val result = service.confirmPasswordReset("expired-token", "NewPass1#")

        assertEquals("Reset token expired", assertFailure(result).reason)
    }

    @Test
    fun confirmPasswordReset_withPasswordTooShort_returnsFailure() = runTest {
        val token = setupValidToken()

        val result = service.confirmPasswordReset(token, "New1#")

        assertEquals("Password must be at least 8 characters", assertFailure(result).reason)
    }

    @Test
    fun confirmPasswordReset_withPasswordNoUppercase_returnsFailure() = runTest {
        val token = setupValidToken()

        val result = service.confirmPasswordReset(token, "newpass1#")

        assertEquals("Password must contain at least one uppercase letter", assertFailure(result).reason)
    }

    @Test
    fun confirmPasswordReset_withPasswordNoLowercase_returnsFailure() = runTest {
        val token = setupValidToken()

        val result = service.confirmPasswordReset(token, "NEWPASS1#")

        assertEquals("Password must contain at least one lowercase letter", assertFailure(result).reason)
    }

    @Test
    fun confirmPasswordReset_withPasswordNoDigit_returnsFailure() = runTest {
        val token = setupValidToken()

        val result = service.confirmPasswordReset(token, "NewPassword#")

        assertEquals("Password must contain at least one digit", assertFailure(result).reason)
    }

    @Test
    fun confirmPasswordReset_withPasswordNoSpecialChar_returnsFailure() = runTest {
        val token = setupValidToken()

        val result = service.confirmPasswordReset(token, "NewPass1")

        assertEquals("Password must contain at least one special character", assertFailure(result).reason)
    }

    @Test
    fun confirmPasswordReset_success_returnsSuccess() = runTest {
        val user = verifiedUser()
        repo.upsertPasswordResetToken(user.id, "token", Instant.now().plus(15, ChronoUnit.MINUTES))

        val result = service.confirmPasswordReset("token", "NewPass1#")

        assertSuccess(result)
    }

    @Test
    fun confirmPasswordReset_success_updatesPasswordHash() = runTest {
        val user = verifiedUser()
        repo.upsertPasswordResetToken(user.id, "token", Instant.now().plus(15, ChronoUnit.MINUTES))

        service.confirmPasswordReset("token", "NewPass1#")

        assertEquals("hashed:NewPass1#", repo.findById(user.id)?.passwordHash)
    }

    @Test
    fun confirmPasswordReset_success_consumesToken() = runTest {
        val user = verifiedUser()
        repo.upsertPasswordResetToken(user.id, "token", Instant.now().plus(15, ChronoUnit.MINUTES))

        service.confirmPasswordReset("token", "NewPass1#")

        assertNull(repo.findPasswordResetToken("token"))
        assertEquals("Invalid reset token", assertFailure(service.confirmPasswordReset("token", "OtherPass1#")).reason)
    }

    @Test
    fun confirmPasswordReset_withRepositoryException_returnsFailure() = runTest {
        repo.throwOnFindPasswordResetToken = true

        val result = service.confirmPasswordReset("token", "NewPass1#")

        assertEquals("findPasswordResetToken failed", assertFailure(result).reason)
    }

    @Test
    fun confirmPasswordReset_passwordValidationDoesNotLeakTokenValidity() = runTest {
        val result = service.confirmPasswordReset("nonexistent-token", "weak")

        assertEquals("Invalid reset token", assertFailure(result).reason)
    }
}