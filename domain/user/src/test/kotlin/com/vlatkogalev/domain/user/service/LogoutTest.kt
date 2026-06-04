package com.vlatkogalev.domain.user.service

import kotlinx.coroutines.runBlocking
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LogoutTest : UserAuthServiceTestBase() {
    @Test
    fun logout_withUnknownId_returnsSuccess() {
        val result = runBlocking { service.logout(UUID.randomUUID()) }
        assertSuccess(result)
    }

    @Test
    fun logout_success_clearsRefreshTokenHash() {
        runBlocking {
            val user = verifiedUser()
            val refreshToken = tokenProvider.generateRefreshToken(user.id)
            repo.saveRefreshTokenHash(user.id, hashToken(refreshToken))

            service.logout(user.id)

            assertNull(runBlocking { repo.findById(user.id)?.refreshTokenHash })
        }
    }

    @Test
    fun logout_withRepositoryException_returnsFailure() {
        runBlocking {
            val user = verifiedUser()
            repo.throwOnClearRefreshTokenHash = true

            val result = service.logout(user.id)

            assertEquals("clearRefreshTokenHash failed", assertFailure(result).reason)
        }
    }
}
