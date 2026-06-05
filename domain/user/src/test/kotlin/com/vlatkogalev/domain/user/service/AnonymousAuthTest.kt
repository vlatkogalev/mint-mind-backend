package com.vlatkogalev.domain.user.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class AnonymousAuthTest : UserAuthServiceTestBase() {
    @Test
    fun authenticateAnonymous_blankInstallationId_returnsFailure() = runTest {
        val result = service.authenticateAnonymous(" ")

        assertEquals("installationId is required", assertFailure(result).reason)
    }

    @Test
    fun authenticateAnonymous_firstLaunch_createsAnonymousUser() = runTest {
        val session = assertSuccess(service.authenticateAnonymous("install-1")).value

        assertTrue(session.accessToken.isNotBlank())
        assertTrue(session.refreshToken.isNotBlank())
        assertTrue(session.user.isAnonymous)
        assertEquals(null, session.user.email)
    }

    @Test
    fun authenticateAnonymous_repeatedLaunch_reusesSameUser() = runTest {
        val first = assertSuccess(service.authenticateAnonymous("install-2")).value
        val second = assertSuccess(service.authenticateAnonymous("install-2")).value

        assertEquals(first.user.id, second.user.id)
        assertNotEquals(first.refreshToken, second.refreshToken)
    }

    @Test
    fun signup_upgradeAnonymous_keepsUserIdAndDisablesAnonymous() = runTest {
        val anonymous = assertSuccess(service.authenticateAnonymous("install-3")).value
        val upgraded = assertSuccess(
            service.signup("upgrade@example.com", TestFixtures.VALID_PASSWORD, anonymous.user.id),
        ).value

        assertEquals(anonymous.user.id, upgraded.user.id)
        assertFalse(upgraded.user.isAnonymous)
        assertNotNull(upgraded.user.upgradedAt)
    }

    @Test
    fun signup_requiresAnonymousUser() = runTest {
        val registered = assertSuccess(
            service.register("registered@example.com", TestFixtures.VALID_PASSWORD, "John", "Doe"),
        ).value

        val result = service.signup("direct@example.com", TestFixtures.VALID_PASSWORD, registered.id)

        assertEquals("Signup upgrade requires anonymous account", assertFailure(result).reason)
    }

    @Test
    fun signup_duplicateEmail_returnsFailure() = runTest {
        val anonymous = assertSuccess(service.authenticateAnonymous("install-4")).value
        assertSuccess(service.signup("dup@example.com", TestFixtures.VALID_PASSWORD, anonymous.user.id))

        val anotherAnonymous = assertSuccess(service.authenticateAnonymous("install-5")).value

        val result = service.signup("dup@example.com", TestFixtures.VALID_PASSWORD, anotherAnonymous.user.id)

        assertEquals("Email already registered", assertFailure(result).reason)
    }
}