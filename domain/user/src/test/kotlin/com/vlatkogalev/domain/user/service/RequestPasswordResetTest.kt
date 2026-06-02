package com.vlatkogalev.domain.user.service

import kotlinx.coroutines.runBlocking
import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RequestPasswordResetTest : UserAuthServiceTestBase() {
    @Test
    fun requestPasswordReset_withUnknownEmail_returnsSuccess() {
        val result = runBlocking { service.requestPasswordReset("missing@example.com") }

        assertSuccess(result)
    }

    @Test
    fun requestPasswordReset_withKnownEmail_returnsSuccess() {
        runBlocking {
            verifiedUser()

            val result = service.requestPasswordReset(TestFixtures.VALID_EMAIL)

            assertSuccess(result)
        }
    }

    @Test
    fun requestPasswordReset_withKnownEmail_storesResetToken() {
        runBlocking {
            val user = verifiedUser()

            assertSuccess(service.requestPasswordReset(TestFixtures.VALID_EMAIL))
            val storedToken = repo.findPasswordResetTokenByUserId(user.id)

            assertNotNull(storedToken)
            assertTrue(storedToken.token.isNotBlank())
        }
    }

    @Test
    fun requestPasswordReset_tokenExpiry_isSet15MinutesFromNow() {
        runBlocking {
            val user = verifiedUser()
            val before = Instant.now().plus(Duration.ofMinutes(15)).minusSeconds(2)

            assertSuccess(service.requestPasswordReset(TestFixtures.VALID_EMAIL))

            val after = Instant.now().plus(Duration.ofMinutes(15)).plusSeconds(2)
            val storedToken = assertNotNull(repo.findPasswordResetTokenByUserId(user.id))
            assertTrue(storedToken.expiresAt.isAfter(before))
            assertTrue(storedToken.expiresAt.isBefore(after))
        }
    }

    @Test
    fun requestPasswordReset_calledTwice_upsertsPreviousToken() {
        runBlocking {
            val user = verifiedUser()

            assertSuccess(service.requestPasswordReset(TestFixtures.VALID_EMAIL))
            val firstToken = assertNotNull(repo.findPasswordResetTokenByUserId(user.id)).token

            assertSuccess(service.requestPasswordReset(TestFixtures.VALID_EMAIL))
            val secondToken = assertNotNull(repo.findPasswordResetTokenByUserId(user.id)).token

            assertNotEquals(firstToken, secondToken)
            assertEquals(null, runBlocking { repo.findPasswordResetToken(firstToken) })
            assertNotNull(runBlocking { repo.findPasswordResetToken(secondToken) })
        }
    }

    @Test
    fun requestPasswordReset_emailIsTrimmedAndLowercased() {
        runBlocking {
            verifiedUser(email = "user@example.com")

            val result = service.requestPasswordReset("  USER@EXAMPLE.COM  ")

            assertSuccess(result)
        }
    }

    @Test
    fun requestPasswordReset_withUnknownEmail_doesNotStoreToken() {
        runBlocking {
            val user = verifiedUser()

            assertSuccess(service.requestPasswordReset("missing@example.com"))

            assertNull(repo.findPasswordResetTokenByUserId(user.id))
        }
    }

    @Test
    fun requestPasswordReset_withRepositoryException_returnsFailure() {
        repo.throwOnFindByEmail = true

        val result = runBlocking { service.requestPasswordReset(TestFixtures.VALID_EMAIL) }

        assertEquals("findByEmail failed", assertFailure(result).reason)
    }
}
