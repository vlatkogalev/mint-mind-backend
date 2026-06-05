package com.vlatkogalev.domain.user.service

import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.test.runTest

class GetUserProfileTest : UserAuthServiceTestBase() {
    @Test
    fun getUserProfile_withUnknownId_returnsFailure() = runTest {
        val result = service.getUserProfile(UUID.randomUUID())

        assertEquals("User not found", assertFailure(result).reason)
    }

    @Test
    fun getUserProfile_success_returnsCorrectFields() = runTest {
        val user = repo.insert(
            TestFixtures.makeAccount(
                emailVerified = true,
                profile = TestFixtures.makeAccount().profile?.copy(avatarUrl = "https://example.com/avatar.png"),
            ),
        )

        val profile = assertSuccess(service.getUserProfile(user.id)).value

        assertEquals(user.id, profile.id)
        assertEquals(user.email, profile.email)
        assertEquals(TestFixtures.VALID_FIRST, profile.firstName)
        assertEquals(TestFixtures.VALID_LAST, profile.lastName)
        assertEquals("https://example.com/avatar.png", profile.avatarUrl)
        assertEquals(user.emailVerified, profile.emailVerified)
        assertEquals(user.isAnonymous, profile.isAnonymous)
    }

    @Test
    fun getUserProfile_withMissingProfile_throwsIllegalState() = runTest {
        val user = repo.insert(TestFixtures.makeAccount(profile = null))

        val failure = assertFailure(service.getUserProfile(user.id))

        assertEquals("User profile is missing", failure.reason)
        assertNotNull(failure.cause)
    }

    @Test
    fun getUserProfile_withRepositoryException_returnsFailure() = runTest {
        repo.throwOnFindById = true

        val result = service.getUserProfile(UUID.randomUUID())

        assertEquals("findById failed", assertFailure(result).reason)
    }
}