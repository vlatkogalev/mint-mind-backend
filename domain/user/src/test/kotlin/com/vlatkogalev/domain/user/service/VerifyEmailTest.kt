package com.vlatkogalev.domain.user.service

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class VerifyEmailTest : UserAuthServiceTestBase() {
    @Test
    fun verifyEmail_withBlankToken_returnsFailure() {
        val result = runBlocking { service.verifyEmail("") }

        assertEquals("Verification token is required", assertFailure(result).reason)
    }

    @Test
    fun verifyEmail_withWhitespaceOnlyToken_returnsFailure() {
        val result = runBlocking { service.verifyEmail("   ") }

        assertEquals("Verification token is required", assertFailure(result).reason)
    }

    @Test
    fun verifyEmail_withUnknownToken_returnsFailure() {
        val result = runBlocking { service.verifyEmail("unknown-token") }

        assertEquals("Invalid verification token", assertFailure(result).reason)
    }

    @Test
    fun verifyEmail_success_returnsSuccess() {
        runBlocking {
            val user = repo.insert(TestFixtures.makeAccount(emailVerified = false, verificationToken = "token"))

            val result = service.verifyEmail(user.verificationToken!!)

            assertSuccess(result)
        }
    }

    @Test
    fun verifyEmail_success_setsEmailVerifiedTrue() {
        runBlocking {
            val user = repo.insert(TestFixtures.makeAccount(emailVerified = false, verificationToken = "token"))

            service.verifyEmail(user.verificationToken!!)

            val identity = assertNotNull(runBlocking { repo.findAuthIdentityByEmail(TestFixtures.VALID_EMAIL) })
            assertEquals(true, runBlocking { repo.findById(identity.userId) }?.emailVerified)
        }
    }

    @Test
    fun verifyEmail_success_clearsVerificationToken() {
        runBlocking {
            val user = repo.insert(TestFixtures.makeAccount(emailVerified = false, verificationToken = "token"))

            service.verifyEmail(user.verificationToken!!)

            val identity2 = assertNotNull(runBlocking { repo.findAuthIdentityByEmail(TestFixtures.VALID_EMAIL) })
            assertNull(runBlocking { repo.findById(identity2.userId) }?.verificationToken)
        }
    }

    @Test
    fun verifyEmail_withRepositoryException_returnsFailure() {
        repo.throwOnVerifyEmail = true

        val result = runBlocking { service.verifyEmail("token") }

        assertEquals("verifyEmail failed", assertFailure(result).reason)
    }
}