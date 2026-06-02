package com.vlatkogalev.domain.user.service

import kotlinx.coroutines.runBlocking
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DeleteAccountTest : UserAuthServiceTestBase() {
    @Test
    fun deleteAccount_withUnknownId_returnsFailure() {
        val result = runBlocking { service.deleteAccount(UUID.randomUUID()) }

        assertEquals("User not found", assertFailure(result).reason)
    }

    @Test
    fun deleteAccount_success_returnsSuccess() {
        runBlocking {
            val user = verifiedUser()

            val result = service.deleteAccount(user.id)

            assertSuccess(result)
        }
    }

    @Test
    fun deleteAccount_success_removesUserFromRepository() {
        runBlocking {
            val user = verifiedUser()

            service.deleteAccount(user.id)

            assertNull(runBlocking { repo.findById(user.id) })
        }
    }

    @Test
    fun deleteAccount_withRepositoryException_returnsFailure() {
        runBlocking {
            val user = verifiedUser()
            repo.throwOnDeleteById = true

            val result = service.deleteAccount(user.id)

            assertEquals("deleteById failed", assertFailure(result).reason)
        }
    }
}
