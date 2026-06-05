package com.vlatkogalev.domain.user.service

import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

class LogoutTest : UserAuthServiceTestBase() {
    @Test
    fun logout_withUnknownId_returnsSuccess() = runTest {
        val result = service.logout(UUID.randomUUID())

        assertSuccess(result)
    }

    @Test
    fun logout_success_clearsRefreshTokenHash() = runTest {
        val user = verifiedUser()
        repo.saveRefreshTokenHash(user.id, "some-hash")

        service.logout(user.id)

        assertEquals(null, repo.findById(user.id)?.refreshTokenHash)
    }

    @Test
    fun logout_withRepositoryException_returnsFailure() = runTest {
        val user = verifiedUser()
        repo.throwOnClearRefreshTokenHash = true

        val result = service.logout(user.id)

        assertEquals("clearRefreshTokenHash failed", assertFailure(result).reason)
    }
}
