package com.vlatkogalev.domain.user.service

import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

class DeleteAccountTest : UserAuthServiceTestBase() {
    @Test
    fun deleteAccount_withUnknownId_returnsFailure() = runTest {
        val result = service.deleteAccount(UUID.randomUUID())

        assertEquals("User not found", assertFailure(result).reason)
    }

    @Test
    fun deleteAccount_success_returnsSuccess() = runTest {
        val user = verifiedUser()

        val result = service.deleteAccount(user.id)

        assertSuccess(result)
    }

    @Test
    fun deleteAccount_success_removesUserFromRepository() = runTest {
        val user = verifiedUser()

        service.deleteAccount(user.id)

        assertNull(repo.findById(user.id))
    }

    @Test
    fun deleteAccount_withRepositoryException_returnsFailure() = runTest {
        val user = verifiedUser()
        repo.throwOnDeleteById = true

        val result = service.deleteAccount(user.id)

        assertEquals("deleteById failed", assertFailure(result).reason)
    }
}
