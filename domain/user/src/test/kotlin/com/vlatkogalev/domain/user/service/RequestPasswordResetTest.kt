package com.vlatkogalev.domain.user.service

import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RequestPasswordResetTest : UserAuthServiceTestBase() {
    @Test
    fun requestPasswordReset_withUnknownEmail_returnsSuccessWithEmptyToken() {
        val result = service.requestPasswordReset("missing@example.com")

        assertEquals("", assertSuccess(result).value.resetToken)
    }

    @Test
    fun requestPasswordReset_withKnownEmail_returnsNonEmptyToken() {
        verifiedUser()

        val result = service.requestPasswordReset(TestFixtures.VALID_EMAIL)

        assertTrue(assertSuccess(result).value.resetToken.isNotBlank())
    }

    @Test
    fun requestPasswordReset_withKnownEmail_storesResetToken() {
        verifiedUser()

        val token = assertSuccess(service.requestPasswordReset(TestFixtures.VALID_EMAIL)).value.resetToken

        assertNotNull(repo.findPasswordResetToken(token))
    }

    @Test
    fun requestPasswordReset_tokenExpiry_isSet15MinutesFromNow() {
        verifiedUser()
        val before = Instant.now().plus(Duration.ofMinutes(15)).minusSeconds(2)

        val token = assertSuccess(service.requestPasswordReset(TestFixtures.VALID_EMAIL)).value.resetToken

        val after = Instant.now().plus(Duration.ofMinutes(15)).plusSeconds(2)
        val storedToken = assertNotNull(repo.findPasswordResetToken(token))
        assertTrue(storedToken.expiresAt.isAfter(before))
        assertTrue(storedToken.expiresAt.isBefore(after))
    }

    @Test
    fun requestPasswordReset_calledTwice_upsertsPreviousToken() {
        verifiedUser()
        val firstToken = assertSuccess(service.requestPasswordReset(TestFixtures.VALID_EMAIL)).value.resetToken

        val secondToken = assertSuccess(service.requestPasswordReset(TestFixtures.VALID_EMAIL)).value.resetToken

        assertNotEquals(firstToken, secondToken)
        assertEquals(null, repo.findPasswordResetToken(firstToken))
        assertNotNull(repo.findPasswordResetToken(secondToken))
    }

    @Test
    fun requestPasswordReset_emailIsTrimmedAndLowercased() {
        verifiedUser(email = "user@example.com")

        val result = service.requestPasswordReset("  USER@EXAMPLE.COM  ")

        assertTrue(assertSuccess(result).value.resetToken.isNotBlank())
    }

    @Test
    fun requestPasswordReset_withRepositoryException_returnsFailure() {
        repo.throwOnFindByEmail = true

        val result = service.requestPasswordReset(TestFixtures.VALID_EMAIL)

        assertEquals("findByEmail failed", assertFailure(result).reason)
    }
}
