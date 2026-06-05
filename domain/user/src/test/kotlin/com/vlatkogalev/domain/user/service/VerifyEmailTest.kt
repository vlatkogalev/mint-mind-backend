package com.vlatkogalev.domain.user.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

class VerifyEmailTest : UserAuthServiceTestBase() {
    @Test
    fun verifyEmail_withBlankToken_returnsFailure() = runTest {
        val result = service.verifyEmail("")

        assertEquals("Verification token is required", assertFailure(result).reason)
    }

    @Test
    fun verifyEmail_withWhitespaceOnlyToken_returnsFailure() = runTest {
        val result = service.verifyEmail("   ")

        assertEquals("Verification token is required", assertFailure(result).reason)
    }

    @Test
    fun verifyEmail_withUnknownToken_returnsFailure() = runTest {
        val result = service.verifyEmail("unknown-token")

        assertEquals("Invalid verification token", assertFailure(result).reason)
    }

    @Test
    fun verifyEmail_success_returnsSuccess() = runTest {
        val user = repo.insert(TestFixtures.makeAccount(emailVerified = false, verificationToken = "token"))

        val result = service.verifyEmail(user.verificationToken!!)

        assertSuccess(result)
    }

    @Test
    fun verifyEmail_success_setsEmailVerifiedTrue() = runTest {
        val user = repo.insert(TestFixtures.makeAccount(emailVerified = false, verificationToken = "token"))

        service.verifyEmail(user.verificationToken!!)

        val identity = assertNotNull(repo.findAuthIdentityByEmail(TestFixtures.VALID_EMAIL))
        assertEquals(true, repo.findById(identity.userId)?.emailVerified)
    }

    @Test
    fun verifyEmail_success_clearsVerificationToken() = runTest {
        val user = repo.insert(TestFixtures.makeAccount(emailVerified = false, verificationToken = "token"))

        service.verifyEmail(user.verificationToken!!)

        val identity2 = assertNotNull(repo.findAuthIdentityByEmail(TestFixtures.VALID_EMAIL))
        assertNull(repo.findById(identity2.userId)?.verificationToken)
    }

    @Test
    fun verifyEmail_withRepositoryException_returnsFailure() = runTest {
        repo.throwOnVerifyEmail = true

        val result = service.verifyEmail("token")

        assertEquals("verifyEmail failed", assertFailure(result).reason)
    }
}
