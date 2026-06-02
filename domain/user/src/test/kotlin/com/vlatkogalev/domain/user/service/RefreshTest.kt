package com.vlatkogalev.domain.user.service

import kotlinx.coroutines.runBlocking
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class RefreshTest : UserAuthServiceTestBase() {
    @Test
    fun refresh_withMalformedToken_noColon_returnsFailure() {
        val result = runBlocking { service.refresh(UUID.randomUUID().toString()) }

        assertEquals("Invalid refresh token", assertFailure(result).reason)
    }

    @Test
    fun refresh_withMalformedToken_invalidUUID_returnsFailure() {
        val result = runBlocking { service.refresh("notauuid:sometoken") }

        assertEquals("Invalid refresh token", assertFailure(result).reason)
    }

    @Test
    fun refresh_withMalformedToken_emptyRawPart_returnsFailure() {
        val result = runBlocking { service.refresh("${UUID.randomUUID()}:") }

        assertEquals("Invalid refresh token", assertFailure(result).reason)
    }

    @Test
    fun refresh_withUnknownUserId_returnsFailure() {
        val result = runBlocking { service.refresh("${UUID.randomUUID()}:refresh-token") }

        assertEquals("Invalid refresh token", assertFailure(result).reason)
    }

    @Test
    fun refresh_withNoStoredTokenHash_returnsFailure() {
        runBlocking {
            val user = verifiedUser()

            val result = service.refresh("${user.id}:refresh-token")

            assertEquals("Invalid refresh token", assertFailure(result).reason)
        }
    }

    @Test
    fun refresh_withTamperedToken_returnsFailure() {
        runBlocking {
            val user = verifiedUser()
            val realToken = "${user.id}:refresh-token"
            repo.saveRefreshTokenHash(user.id, hashToken(realToken))

            val result = service.refresh("${user.id}:tampered-token")

            assertEquals("Invalid refresh token", assertFailure(result).reason)
        }
    }

    @Test
    fun refresh_success_returnsNewLoginSession() {
        runBlocking {
            val user = verifiedUser()
            val loginSession = assertSuccess(service.login(TestFixtures.VALID_EMAIL, TestFixtures.VALID_PASSWORD)).value

            val refreshed = assertSuccess(service.refresh(loginSession.refreshToken)).value

            assertEquals("access:${user.id}", refreshed.accessToken)
            assertTrue(refreshed.refreshToken.startsWith("${user.id}:"))
            assertEquals(3600L, refreshed.accessTokenExpiresInSeconds)
            assertEquals(86400L, refreshed.refreshTokenExpiresInSeconds)
        }
    }

    @Test
    fun refresh_success_rotatesRefreshToken() {
        runBlocking {
            verifiedUser()
            val loginSession = assertSuccess(service.login(TestFixtures.VALID_EMAIL, TestFixtures.VALID_PASSWORD)).value

            val refreshed = assertSuccess(service.refresh(loginSession.refreshToken)).value

            assertNotEquals(loginSession.refreshToken, refreshed.refreshToken)
        }
    }

    @Test
    fun refresh_success_updatesStoredHash() {
        runBlocking {
            val user = verifiedUser()
            val loginSession = assertSuccess(service.login(TestFixtures.VALID_EMAIL, TestFixtures.VALID_PASSWORD)).value
            val originalHash = runBlocking { repo.findById(user.id) }?.refreshTokenHash

            val refreshed = assertSuccess(service.refresh(loginSession.refreshToken)).value

            val userAfter = runBlocking { repo.findById(user.id) }
            assertNotEquals(originalHash, userAfter?.refreshTokenHash)
            assertEquals(hashToken(refreshed.refreshToken), userAfter?.refreshTokenHash)
        }
    }

    @Test
    fun refresh_success_oldTokenRejectedAfterRotation() {
        runBlocking {
            verifiedUser()
            val loginSession = assertSuccess(service.login(TestFixtures.VALID_EMAIL, TestFixtures.VALID_PASSWORD)).value
            assertSuccess(service.refresh(loginSession.refreshToken))

            val result = service.refresh(loginSession.refreshToken)

            assertEquals("Invalid refresh token", assertFailure(result).reason)
        }
    }

    @Test
    fun refresh_withRepositoryException_returnsFailure() {
        runBlocking {
            val user = verifiedUser()
            val token = "${user.id}:refresh-token"
            repo.saveRefreshTokenHash(user.id, hashToken(token))
            repo.throwOnFindById = true

            val result = service.refresh(token)

            assertEquals("findById failed", assertFailure(result).reason)
        }
    }
}
