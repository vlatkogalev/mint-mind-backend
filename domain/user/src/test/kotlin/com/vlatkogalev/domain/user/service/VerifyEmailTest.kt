package com.vlatkogalev.domain.user.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VerifyEmailTest : UserAuthServiceTestBase() {
    @Test
    fun verifyEmail_withBlankToken_returnsFailure() {
        val result = service.verifyEmail("")

        assertEquals("Verification token is required", assertFailure(result).reason)
    }

    @Test
    fun verifyEmail_withWhitespaceOnlyToken_returnsFailure() {
        val result = service.verifyEmail("   ")

        assertEquals("Verification token is required", assertFailure(result).reason)
    }

    @Test
    fun verifyEmail_withUnknownToken_returnsFailure() {
        val result = service.verifyEmail("unknown-token")

        assertEquals("Invalid verification token", assertFailure(result).reason)
    }

    @Test
    fun verifyEmail_success_returnsSuccess() {
        val user = repo.insert(TestFixtures.makeAccount(emailVerified = false, verificationToken = "token"))

        val result = service.verifyEmail(user.verificationToken!!)

        assertSuccess(result)
    }

    @Test
    fun verifyEmail_success_setsEmailVerifiedTrue() {
        val user = repo.insert(TestFixtures.makeAccount(emailVerified = false, verificationToken = "token"))

        service.verifyEmail(user.verificationToken!!)

        assertTrue(repo.findByEmail(TestFixtures.VALID_EMAIL)?.emailVerified == true)
    }

    @Test
    fun verifyEmail_success_clearsVerificationToken() {
        val user = repo.insert(TestFixtures.makeAccount(emailVerified = false, verificationToken = "token"))

        service.verifyEmail(user.verificationToken!!)

        assertNull(repo.findByEmail(TestFixtures.VALID_EMAIL)?.verificationToken)
    }

    @Test
    fun verifyEmail_withRepositoryException_returnsFailure() {
        repo.throwOnVerifyEmail = true

        val result = service.verifyEmail("token")

        assertEquals("verifyEmail failed", assertFailure(result).reason)
    }
}
