package com.vlatkogalev.domain.user.service

import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ResendVerificationTest : UserAuthServiceTestBase() {
    @Test
    fun resendVerification_withUnknownEmail_returnsSuccessSilently() {
        val result = service.resendVerification("missing@example.com")

        assertSuccess(result)
        assertTrue(emailSender.sentEmails.isEmpty())
    }

    @Test
    fun resendVerification_withAlreadyVerifiedEmail_returnsSuccessSilently() {
        verifiedUser()

        val result = service.resendVerification(TestFixtures.VALID_EMAIL)

        assertSuccess(result)
        assertTrue(emailSender.sentEmails.isEmpty())
    }

    @Test
    fun resendVerification_withNoPreviousSend_sendsEmail() {
        repo.insert(TestFixtures.makeAccount(verificationEmailSentAt = null))

        val result = service.resendVerification(TestFixtures.VALID_EMAIL)

        assertSuccess(result)
        assertEquals(1, emailSender.sentEmails.size)
        assertEquals(TestFixtures.VALID_EMAIL, emailSender.sentEmails.single().first)
    }

    @Test
    fun resendVerification_withNoPreviousSend_updatesVerificationToken() {
        val user = repo.insert(TestFixtures.makeAccount(verificationToken = "old-token", verificationEmailSentAt = null))

        service.resendVerification(TestFixtures.VALID_EMAIL)

        val stored = assertNotNull(repo.findById(user.id))
        assertNotEquals("old-token", stored.verificationToken)
        assertEquals(stored.verificationToken, emailSender.sentEmails.single().second)
        assertNotNull(stored.verificationEmailSentAt)
    }

    @Test
    fun resendVerification_withinCooldown_returnsFailureWithWaitMinutes() {
        repo.insert(TestFixtures.makeAccount(verificationEmailSentAt = Instant.now().minus(5, ChronoUnit.MINUTES)))

        val result = service.resendVerification(TestFixtures.VALID_EMAIL)

        assertTrue(assertFailure(result).reason.contains("Please wait"))
        assertTrue(emailSender.sentEmails.isEmpty())
    }

    @Test
    fun resendVerification_atExactCooldownBoundary_returnsFailure() {
        repo.insert(TestFixtures.makeAccount(verificationEmailSentAt = Instant.now().minus(9, ChronoUnit.MINUTES)))

        val result = service.resendVerification(TestFixtures.VALID_EMAIL)

        assertTrue(assertFailure(result).reason.contains("Please wait"))
        assertTrue(emailSender.sentEmails.isEmpty())
    }

    @Test
    fun resendVerification_afterCooldownExpired_sendsEmail() {
        repo.insert(TestFixtures.makeAccount(verificationEmailSentAt = Instant.now().minus(11, ChronoUnit.MINUTES)))

        val result = service.resendVerification(TestFixtures.VALID_EMAIL)

        assertSuccess(result)
        assertEquals(1, emailSender.sentEmails.size)
    }

    @Test
    fun resendVerification_afterCooldownExpired_sendsNewToken() {
        val user = repo.insert(TestFixtures.makeAccount(verificationToken = "old-token", verificationEmailSentAt = Instant.now().minus(11, ChronoUnit.MINUTES)))

        service.resendVerification(TestFixtures.VALID_EMAIL)

        val stored = assertNotNull(repo.findById(user.id))
        assertNotEquals("old-token", stored.verificationToken)
        assertEquals(stored.verificationToken, emailSender.sentEmails.single().second)
    }

    @Test
    fun resendVerification_emailIsTrimmedAndLowercased() {
        repo.insert(TestFixtures.makeAccount(email = "user@example.com", verificationEmailSentAt = null))

        val result = service.resendVerification("  USER@EXAMPLE.COM  ")

        assertSuccess(result)
        assertEquals("user@example.com", emailSender.sentEmails.single().first)
    }

    @Test
    fun resendVerification_withRepositoryException_returnsFailure() {
        repo.throwOnFindByEmail = true

        val result = service.resendVerification(TestFixtures.VALID_EMAIL)

        assertEquals("findByEmail failed", assertFailure(result).reason)
    }

    @Test
    fun resendVerification_atExactCooldownExpiry_sendsEmail() {
        repo.insert(TestFixtures.makeAccount(verificationEmailSentAt = Instant.now().minus(10, ChronoUnit.MINUTES)))

        val result = service.resendVerification(TestFixtures.VALID_EMAIL)

        assertSuccess(result)
        assertEquals(1, emailSender.sentEmails.size)
    }
}
