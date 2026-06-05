package com.vlatkogalev.domain.user.service

import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class RequestPasswordResetTest : UserAuthServiceTestBase() {
    @Test
    fun requestPasswordReset_withUnknownEmail_returnsSuccess() = runTest {
        val result = service.requestPasswordReset("missing@example.com")

        assertSuccess(result)
    }

    @Test
    fun requestPasswordReset_withKnownEmail_returnsSuccess() = runTest {
        verifiedUser()

        val result = service.requestPasswordReset(TestFixtures.VALID_EMAIL)

        assertSuccess(result)
    }

    @Test
    fun requestPasswordReset_withKnownEmail_storesResetToken() = runTest {
        val user = verifiedUser()

        assertSuccess(service.requestPasswordReset(TestFixtures.VALID_EMAIL))
        val storedToken = repo.findPasswordResetTokenByUserId(user.id)

        assertNotNull(storedToken)
        assertTrue(storedToken.token.isNotBlank())
    }

    @Test
    fun requestPasswordReset_tokenExpiry_isSet15MinutesFromNow() = runTest {
        val user = verifiedUser()
        val before = Instant.now().plus(Duration.ofMinutes(15)).minusSeconds(2)

        assertSuccess(service.requestPasswordReset(TestFixtures.VALID_EMAIL))

        val after = Instant.now().plus(Duration.ofMinutes(15)).plusSeconds(2)
        val storedToken = assertNotNull(repo.findPasswordResetTokenByUserId(user.id))
        assertTrue(storedToken.expiresAt.isAfter(before))
        assertTrue(storedToken.expiresAt.isBefore(after))
    }

    @Test
    fun requestPasswordReset_calledTwice_upsertsPreviousToken() = runTest {
        val user = verifiedUser()

        assertSuccess(service.requestPasswordReset(TestFixtures.VALID_EMAIL))
        val firstToken = assertNotNull(repo.findPasswordResetTokenByUserId(user.id)).token

        assertSuccess(service.requestPasswordReset(TestFixtures.VALID_EMAIL))
        val secondToken = assertNotNull(repo.findPasswordResetTokenByUserId(user.id)).token

        assertNotEquals(firstToken, secondToken)
        assertEquals(null, repo.findPasswordResetToken(firstToken))
        assertNotNull(repo.findPasswordResetToken(secondToken))
    }

    @Test
    fun requestPasswordReset_emailIsTrimmedAndLowercased() = runTest {
        verifiedUser(email = "user@example.com")

        val result = service.requestPasswordReset("  USER@EXAMPLE.COM  ")

        assertSuccess(result)
    }

    @Test
    fun requestPasswordReset_withUnknownEmail_doesNotStoreToken() = runTest {
        val user = verifiedUser()

        assertSuccess(service.requestPasswordReset("missing@example.com"))

        assertNull(repo.findPasswordResetTokenByUserId(user.id))
    }

    @Test
    fun requestPasswordReset_withRepositoryException_returnsFailure() = runTest {
        repo.throwOnFindByEmail = true

        val result = service.requestPasswordReset(TestFixtures.VALID_EMAIL)

        assertEquals("findByEmail failed", assertFailure(result).reason)
    }
}