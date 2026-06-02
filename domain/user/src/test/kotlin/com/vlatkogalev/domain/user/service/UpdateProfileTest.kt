package com.vlatkogalev.domain.user.service

import kotlinx.coroutines.runBlocking
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class UpdateProfileTest : UserAuthServiceTestBase() {
    @Test
    fun updateProfile_withBlankFirstName_returnsFailure() {
        val result = runBlocking { service.updateProfile(UUID.randomUUID(), "", TestFixtures.VALID_LAST) }

        assertEquals("First name is required", assertFailure(result).reason)
    }

    @Test
    fun updateProfile_withFirstNameOver50Chars_returnsFailure() {
        val result = runBlocking { service.updateProfile(UUID.randomUUID(), "J".repeat(51), TestFixtures.VALID_LAST) }

        assertEquals("First name must be 50 characters or fewer", assertFailure(result).reason)
    }

    @Test
    fun updateProfile_withBlankLastName_returnsFailure() {
        val result = runBlocking { service.updateProfile(UUID.randomUUID(), TestFixtures.VALID_FIRST, "") }

        assertEquals("Last name is required", assertFailure(result).reason)
    }

    @Test
    fun updateProfile_withLastNameOver50Chars_returnsFailure() {
        val result = runBlocking { service.updateProfile(UUID.randomUUID(), TestFixtures.VALID_FIRST, "D".repeat(51)) }

        assertEquals("Last name must be 50 characters or fewer", assertFailure(result).reason)
    }

    @Test
    fun updateProfile_withUnknownUserId_returnsFailure() {
        val result = runBlocking { service.updateProfile(UUID.randomUUID(), "Jane", "Doe") }

        assertEquals("User not found", assertFailure(result).reason)
    }

    @Test
    fun updateProfile_success_returnsUpdatedUser() {
        runBlocking {
            val user = verifiedUser()

            val updated = assertSuccess(service.updateProfile(user.id, "Jane", "Smith")).value

            assertEquals("Jane", updated.firstName)
            assertEquals("Smith", updated.lastName)
        }
    }

    @Test
    fun updateProfile_success_trimsWhitespace() {
        runBlocking {
            val user = verifiedUser()

            val updated = assertSuccess(service.updateProfile(user.id, "  Jane  ", "  Smith  ")).value

            assertEquals("Jane", updated.firstName)
            assertEquals("Smith", updated.lastName)
            val stored = runBlocking { repo.findById(user.id) }
            assertEquals("Jane", stored?.profile?.firstName)
            assertEquals("Smith", stored?.profile?.lastName)
        }
    }

    @Test
    fun updateProfile_withRepositoryException_returnsFailure() {
        runBlocking {
            val user = verifiedUser()
            repo.throwOnUpdateProfile = true

            val result = service.updateProfile(user.id, "Jane", "Smith")

            assertEquals("updateProfile failed", assertFailure(result).reason)
        }
    }
}
