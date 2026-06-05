package com.vlatkogalev.domain.user.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class LoginTest : UserAuthServiceTestBase() {
    @Test
    fun login_withUnknownEmail_returnsFailure() = runTest {
        val result = service.login(TestFixtures.VALID_EMAIL, TestFixtures.VALID_PASSWORD)

        assertEquals("Invalid email or password", assertFailure(result).reason)
    }

    @Test
    fun login_withWrongPassword_returnsFailure() = runTest {
        verifiedUser()

        val result = service.login(TestFixtures.VALID_EMAIL, "WrongPass1#")

        assertEquals("Invalid email or password", assertFailure(result).reason)
    }

    @Test
    fun login_withUnverifiedEmail_returnsFailure() = runTest {
        repo.insert(TestFixtures.makeAccount(emailVerified = false))

        val result = service.login(TestFixtures.VALID_EMAIL, TestFixtures.VALID_PASSWORD)

        assertEquals("Email verification required", assertFailure(result).reason)
    }

    @Test
    fun login_success_returnsLoginSession() = runTest {
        verifiedUser()

        val session = assertSuccess(service.login(TestFixtures.VALID_EMAIL, TestFixtures.VALID_PASSWORD)).value

        assertTrue(session.accessToken.isNotBlank())
        assertTrue(session.refreshToken.isNotBlank())
        assertEquals(3600L, session.accessTokenExpiresInSeconds)
        assertEquals(86400L, session.refreshTokenExpiresInSeconds)
    }

    @Test
    fun login_success_accessTokenContainsUserId() = runTest {
        val user = verifiedUser()

        val session = assertSuccess(service.login(TestFixtures.VALID_EMAIL, TestFixtures.VALID_PASSWORD)).value

        assertEquals("access:${user.id}", session.accessToken)
    }

    @Test
    fun login_success_refreshTokenContainsUserId() = runTest {
        val user = verifiedUser()

        val session = assertSuccess(service.login(TestFixtures.VALID_EMAIL, TestFixtures.VALID_PASSWORD)).value

        assertTrue(session.refreshToken.startsWith("${user.id}:"))
    }

    @Test
    fun login_success_savesRefreshTokenHash() = runTest {
        val user = verifiedUser()

        service.login(TestFixtures.VALID_EMAIL, TestFixtures.VALID_PASSWORD)

        assertNotNull(repo.findById(user.id)?.refreshTokenHash)
    }

    @Test
    fun login_success_hashesRefreshTokenBeforeStoring() = runTest {
        val user = verifiedUser()

        val session = assertSuccess(service.login(TestFixtures.VALID_EMAIL, TestFixtures.VALID_PASSWORD)).value

        assertNotEquals(session.refreshToken, repo.findById(user.id)?.refreshTokenHash)
        assertEquals(hashToken(session.refreshToken), repo.findById(user.id)?.refreshTokenHash)
    }

    @Test
    fun login_emailIsTrimmedAndLowercased() = runTest {
        verifiedUser(email = "user@example.com")

        val result = service.login("  USER@EXAMPLE.COM  ", TestFixtures.VALID_PASSWORD)

        assertSuccess(result)
    }

    @Test
    fun login_withRepositoryException_returnsFailure() = runTest {
        repo.throwOnFindByEmail = true

        val result = service.login(TestFixtures.VALID_EMAIL, TestFixtures.VALID_PASSWORD)

        assertEquals("findByEmail failed", assertFailure(result).reason)
    }
}
