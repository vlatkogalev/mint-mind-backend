package com.vlatkogalev.domain.user.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RegisterTest : UserAuthServiceTestBase() {
    @Test
    fun register_withBlankEmail_returnsFailure() {
        val result = service.register("", TestFixtures.VALID_PASSWORD, TestFixtures.VALID_FIRST, TestFixtures.VALID_LAST)

        assertEquals("Invalid email", assertFailure(result).reason)
    }

    @Test
    fun register_withInvalidEmailFormat_returnsFailure() {
        listOf("notanemail", "@nodomain", "missing@", "spaces in@email.com").forEach { invalidEmail ->
            val result = service.register(invalidEmail, TestFixtures.VALID_PASSWORD, TestFixtures.VALID_FIRST, TestFixtures.VALID_LAST)

            assertEquals("Invalid email", assertFailure(result).reason)
        }
    }

    @Test
    fun register_withValidEmail_trimsAndLowercases() {
        val result = service.register("  TEST@Example.COM  ", TestFixtures.VALID_PASSWORD, TestFixtures.VALID_FIRST, TestFixtures.VALID_LAST)

        assertSuccess(result)
        assertNotNull(repo.findById(repo.findAuthIdentityByEmail("test@example.com")!!.userId))
    }

    @Test
    fun register_withBlankFirstName_returnsFailure() {
        val result = service.register(TestFixtures.VALID_EMAIL, TestFixtures.VALID_PASSWORD, "", TestFixtures.VALID_LAST)

        assertEquals("First name is required", assertFailure(result).reason)
    }

    @Test
    fun register_withWhitespaceOnlyFirstName_returnsFailure() {
        val result = service.register(TestFixtures.VALID_EMAIL, TestFixtures.VALID_PASSWORD, "   ", TestFixtures.VALID_LAST)

        assertEquals("First name is required", assertFailure(result).reason)
    }

    @Test
    fun register_withFirstNameExactly50Chars_succeeds() {
        val firstName = "J".repeat(50)
        val result = service.register(TestFixtures.VALID_EMAIL, TestFixtures.VALID_PASSWORD, firstName, TestFixtures.VALID_LAST)

        assertEquals(firstName, assertSuccess(result).value.firstName)
    }

    @Test
    fun register_withFirstNameOver50Chars_returnsFailure() {
        val result = service.register(TestFixtures.VALID_EMAIL, TestFixtures.VALID_PASSWORD, "J".repeat(51), TestFixtures.VALID_LAST)

        assertEquals("First name must be 50 characters or fewer", assertFailure(result).reason)
    }

    @Test
    fun register_withBlankLastName_returnsFailure() {
        val result = service.register(TestFixtures.VALID_EMAIL, TestFixtures.VALID_PASSWORD, TestFixtures.VALID_FIRST, "")

        assertEquals("Last name is required", assertFailure(result).reason)
    }

    @Test
    fun register_withLastNameOver50Chars_returnsFailure() {
        val result = service.register(TestFixtures.VALID_EMAIL, TestFixtures.VALID_PASSWORD, TestFixtures.VALID_FIRST, "D".repeat(51))

        assertEquals("Last name must be 50 characters or fewer", assertFailure(result).reason)
    }

    @Test
    fun register_withPasswordTooShort_returnsFailure() {
        val result = service.register(TestFixtures.VALID_EMAIL, "Pas123#", TestFixtures.VALID_FIRST, TestFixtures.VALID_LAST)

        assertEquals("Password must be at least 8 characters", assertFailure(result).reason)
    }

    @Test
    fun register_withPasswordNoUppercase_returnsFailure() {
        val result = service.register(TestFixtures.VALID_EMAIL, "pass12345#", TestFixtures.VALID_FIRST, TestFixtures.VALID_LAST)

        assertEquals("Password must contain at least one uppercase letter", assertFailure(result).reason)
    }

    @Test
    fun register_withPasswordNoLowercase_returnsFailure() {
        val result = service.register(TestFixtures.VALID_EMAIL, "PASS12345#", TestFixtures.VALID_FIRST, TestFixtures.VALID_LAST)

        assertEquals("Password must contain at least one lowercase letter", assertFailure(result).reason)
    }

    @Test
    fun register_withPasswordNoDigit_returnsFailure() {
        val result = service.register(TestFixtures.VALID_EMAIL, "Password#", TestFixtures.VALID_FIRST, TestFixtures.VALID_LAST)

        assertEquals("Password must contain at least one digit", assertFailure(result).reason)
    }

    @Test
    fun register_withPasswordNoSpecialChar_returnsFailure() {
        val result = service.register(TestFixtures.VALID_EMAIL, "Password1", TestFixtures.VALID_FIRST, TestFixtures.VALID_LAST)

        assertEquals("Password must contain at least one special character", assertFailure(result).reason)
    }

    @Test
    fun register_withPasswordExactly8Chars_andAllRules_succeeds() {
        val result = service.register(TestFixtures.VALID_EMAIL, "Pass123#", TestFixtures.VALID_FIRST, TestFixtures.VALID_LAST)

        assertSuccess(result)
    }

    @Test
    fun register_withDuplicateEmail_returnsFailure() {
        repo.insert(TestFixtures.makeAccount(email = TestFixtures.VALID_EMAIL))

        val result = service.register(TestFixtures.VALID_EMAIL, TestFixtures.VALID_PASSWORD, TestFixtures.VALID_FIRST, TestFixtures.VALID_LAST)

        assertEquals("Email already registered", assertFailure(result).reason)
    }

    @Test
    fun register_success_returnsUserWithCorrectFields() {
        val result = service.register(TestFixtures.VALID_EMAIL, TestFixtures.VALID_PASSWORD, TestFixtures.VALID_FIRST, TestFixtures.VALID_LAST)

        val user = assertSuccess(result).value
        assertEquals(TestFixtures.VALID_EMAIL, user.email)
        assertEquals(TestFixtures.VALID_FIRST, user.firstName)
        assertEquals(TestFixtures.VALID_LAST, user.lastName)
        assertFalse(user.emailVerified)
    }

    @Test
    fun register_success_storesHashedPassword() {
        service.register(TestFixtures.VALID_EMAIL, TestFixtures.VALID_PASSWORD, TestFixtures.VALID_FIRST, TestFixtures.VALID_LAST)

        val identity = assertNotNull(repo.findAuthIdentityByEmail(TestFixtures.VALID_EMAIL))
        val stored = assertNotNull(repo.findById(identity.userId))
        assertTrue(assertNotNull(stored.passwordHash).startsWith("hashed:"))
        assertEquals("hashed:${TestFixtures.VALID_PASSWORD}", stored.passwordHash)
    }

    @Test
    fun register_success_setsEmailVerifiedFalse() {
        service.register(TestFixtures.VALID_EMAIL, TestFixtures.VALID_PASSWORD, TestFixtures.VALID_FIRST, TestFixtures.VALID_LAST)

        val identity2 = assertNotNull(repo.findAuthIdentityByEmail(TestFixtures.VALID_EMAIL))
        assertFalse(assertNotNull(repo.findById(identity2.userId)).emailVerified)
    }

    @Test
    fun register_success_sendsVerificationEmail() {
        service.register(TestFixtures.VALID_EMAIL, TestFixtures.VALID_PASSWORD, TestFixtures.VALID_FIRST, TestFixtures.VALID_LAST)

        assertEquals(1, emailSender.sentEmails.size)
        assertEquals(TestFixtures.VALID_EMAIL, emailSender.sentEmails.single().first)
        assertTrue(emailSender.sentEmails.single().second.isNotBlank())
    }

    @Test
    fun register_withSkipVerification_setsEmailVerifiedTrue() {
        val result = skipVerificationService().register(TestFixtures.VALID_EMAIL, TestFixtures.VALID_PASSWORD, TestFixtures.VALID_FIRST, TestFixtures.VALID_LAST)

        assertFalse(assertSuccess(result).value.emailVerified)

        val identity3 = assertNotNull(repo.findAuthIdentityByEmail(TestFixtures.VALID_EMAIL))
        assertTrue(assertNotNull(repo.findById(identity3.userId)).emailVerified)
    }

    @Test
    fun register_withSkipVerification_doesNotSendEmail() {
        skipVerificationService().register(TestFixtures.VALID_EMAIL, TestFixtures.VALID_PASSWORD, TestFixtures.VALID_FIRST, TestFixtures.VALID_LAST)

        assertTrue(emailSender.sentEmails.isEmpty())
    }

    @Test
    fun register_withRepositoryException_returnsFailure() {
        repo.throwOnCreate = true

        val result = service.register(TestFixtures.VALID_EMAIL, TestFixtures.VALID_PASSWORD, TestFixtures.VALID_FIRST, TestFixtures.VALID_LAST)

        assertEquals("create failed", assertFailure(result).reason)
    }
}