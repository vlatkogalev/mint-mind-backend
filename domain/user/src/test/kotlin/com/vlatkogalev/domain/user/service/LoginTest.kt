package com.vlatkogalev.domain.user.service

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LoginTest : UserAuthServiceTestBase() {
    @Test
    fun login_withUnknownEmail_returnsFailure() {
        val result = runBlocking { service.login(TestFixtures.VALID_EMAIL, TestFixtures.VALID_PASSWORD) }

        assertEquals("Invalid email or password", assertFailure(result).reason)
    }

    @Test
    fun login_withWrongPassword_returnsFailure() {
        runBlocking {
            verifiedUser()

            val result = service.login(TestFixtures.VALID_EMAIL, "WrongPass1#")

            assertEquals("Invalid email or password", assertFailure(result).reason)
        }
    }

    @Test
    fun login_withUnverifiedEmail_returnsFailure() {
        runBlocking {
            repo.insert(TestFixtures.makeAccount(emailVerified = false))

            val result = service.login(TestFixtures.VALID_EMAIL, TestFixtures.VALID_PASSWORD)

            assertEquals("Email verification required", assertFailure(result).reason)
        }
    }

    @Test
    fun login_success_returnsLoginSession() {
        runBlocking {
            verifiedUser()

            val session = assertSuccess(service.login(TestFixtures.VALID_EMAIL, TestFixtures.VALID_PASSWORD)).value

            assertTrue(session.accessToken.isNotBlank())
            assertTrue(session.refreshToken.isNotBlank())
            assertEquals(3600L, session.accessTokenExpiresInSeconds)
            assertEquals(86400L, session.refreshTokenExpiresInSeconds)
        }
    }

    @Test
    fun login_success_accessTokenContainsUserId() {
        runBlocking {
            val user = verifiedUser()

            val session = assertSuccess(service.login(TestFixtures.VALID_EMAIL, TestFixtures.VALID_PASSWORD)).value

            assertEquals("access:${user.id}", session.accessToken)
        }
    }

    @Test
    fun login_success_refreshTokenContainsUserId() {
        runBlocking {
            val user = verifiedUser()

            val session = assertSuccess(service.login(TestFixtures.VALID_EMAIL, TestFixtures.VALID_PASSWORD)).value

            assertTrue(session.refreshToken.startsWith("${user.id}:"))
        }
    }

    @Test
    fun login_success_savesRefreshTokenHash() {
        runBlocking {
            val user = verifiedUser()

            service.login(TestFixtures.VALID_EMAIL, TestFixtures.VALID_PASSWORD)

            assertNotNull(runBlocking { repo.findById(user.id) }?.refreshTokenHash)
        }
    }

    @Test
    fun login_success_hashesRefreshTokenBeforeStoring() {
        runBlocking {
            val user = verifiedUser()

            val session = assertSuccess(service.login(TestFixtures.VALID_EMAIL, TestFixtures.VALID_PASSWORD)).value

            val storedUser = runBlocking { repo.findById(user.id) }
            assertNotEquals(session.refreshToken, storedUser?.refreshTokenHash)
            assertEquals(hashToken(session.refreshToken), storedUser?.refreshTokenHash)
        }
    }

    @Test
    fun login_emailIsTrimmedAndLowercased() {
        runBlocking {
            verifiedUser(email = "user@example.com")

            val result = service.login("  USER@EXAMPLE.COM  ", TestFixtures.VALID_PASSWORD)

            assertSuccess(result)
        }
    }

    @Test
    fun login_withRepositoryException_returnsFailure() {
        repo.throwOnFindByEmail = true

        val result = runBlocking { service.login(TestFixtures.VALID_EMAIL, TestFixtures.VALID_PASSWORD) }

        assertEquals("findByEmail failed", assertFailure(result).reason)
    }
}
