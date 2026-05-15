package com.vlatkogalev.domain.user.service

import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DeleteAccountTest : UserAuthServiceTestBase() {
    @Test
    fun deleteAccount_withUnknownId_returnsFailure() {
        val result = service.deleteAccount(UUID.randomUUID())

        assertEquals("User not found", assertFailure(result).reason)
    }

    @Test
    fun deleteAccount_success_returnsSuccess() {
        val user = verifiedUser()

        val result = service.deleteAccount(user.id)

        assertSuccess(result)
    }

    @Test
    fun deleteAccount_success_removesUserFromRepository() {
        val user = verifiedUser()

        service.deleteAccount(user.id)

        assertNull(repo.findById(user.id))
    }

    @Test
    fun deleteAccount_withRepositoryException_returnsFailure() {
        val user = verifiedUser()
        repo.throwOnDeleteById = true

        val result = service.deleteAccount(user.id)

        assertEquals("deleteById failed", assertFailure(result).reason)
    }
}
