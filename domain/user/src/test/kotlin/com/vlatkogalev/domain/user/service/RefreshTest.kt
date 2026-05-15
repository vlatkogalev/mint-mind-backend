package com.vlatkogalev.domain.user.service

import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class RefreshTest : UserAuthServiceTestBase() {
    @Test
    fun refresh_withMalformedToken_noColon_returnsFailure() {
        val result = service.refresh(UUID.randomUUID().toString())

        assertEquals("Invalid refresh token", assertFailure(result).reason)
    }

    @Test
    fun refresh_withMalformedToken_invalidUUID_returnsFailure() {
        val result = service.refresh("notauuid:sometoken")

        assertEquals("Invalid refresh token", assertFailure(result).reason)
    }

    @Test
    fun refresh_withMalformedToken_emptyRawPart_returnsFailure() {
        val result = service.refresh("${UUID.randomUUID()}:")

        assertEquals("Invalid refresh token", assertFailure(result).reason)
    }

    @Test
    fun refresh_withUnknownUserId_returnsFailure() {
        val result = service.refresh("${UUID.randomUUID()}:refresh-token")

        assertEquals("Invalid refresh token", assertFailure(result).reason)
    }

    @Test
    fun refresh_withNoStoredTokenHash_returnsFailure() {
        val user = verifiedUser()

        val result = service.refresh("${user.id}:refresh-token")

        assertEquals("Invalid refresh token", assertFailure(result).reason)
    }

    @Test
    fun refresh_withTamperedToken_returnsFailure() {
        val user = verifiedUser()
        val realToken = "${user.id}:refresh-token"
        repo.saveRefreshTokenHash(user.id, hashToken(realToken))

        val result = service.refresh("${user.id}:tampered-token")

        assertEquals("Invalid refresh token", assertFailure(result).reason)
    }

    @Test
    fun refresh_success_returnsNewLoginSession() {
        val user = verifiedUser()
        val loginSession = assertSuccess(service.login(TestFixtures.VALID_EMAIL, TestFixtures.VALID_PASSWORD)).value

        val refreshed = assertSuccess(service.refresh(loginSession.refreshToken)).value

        assertEquals("access:${user.id}", refreshed.accessToken)
        assertTrue(refreshed.refreshToken.startsWith("${user.id}:"))
        assertEquals(3600L, refreshed.accessTokenExpiresInSeconds)
        assertEquals(86400L, refreshed.refreshTokenExpiresInSeconds)
    }

    @Test
    fun refresh_success_rotatesRefreshToken() {
        verifiedUser()
        val loginSession = assertSuccess(service.login(TestFixtures.VALID_EMAIL, TestFixtures.VALID_PASSWORD)).value

        val refreshed = assertSuccess(service.refresh(loginSession.refreshToken)).value

        assertNotEquals(loginSession.refreshToken, refreshed.refreshToken)
    }

    @Test
    fun refresh_success_updatesStoredHash() {
        val user = verifiedUser()
        val loginSession = assertSuccess(service.login(TestFixtures.VALID_EMAIL, TestFixtures.VALID_PASSWORD)).value
        val originalHash = repo.findById(user.id)?.refreshTokenHash

        val refreshed = assertSuccess(service.refresh(loginSession.refreshToken)).value

        assertNotEquals(originalHash, repo.findById(user.id)?.refreshTokenHash)
        assertEquals(hashToken(refreshed.refreshToken), repo.findById(user.id)?.refreshTokenHash)
    }

    @Test
    fun refresh_success_oldTokenRejectedAfterRotation() {
        verifiedUser()
        val loginSession = assertSuccess(service.login(TestFixtures.VALID_EMAIL, TestFixtures.VALID_PASSWORD)).value
        assertSuccess(service.refresh(loginSession.refreshToken))

        val result = service.refresh(loginSession.refreshToken)

        assertEquals("Invalid refresh token", assertFailure(result).reason)
    }

    @Test
    fun refresh_withRepositoryException_returnsFailure() {
        val user = verifiedUser()
        val token = "${user.id}:refresh-token"
        repo.saveRefreshTokenHash(user.id, hashToken(token))
        repo.throwOnFindById = true

        val result = service.refresh(token)

        assertEquals("findById failed", assertFailure(result).reason)
    }
}
