package com.vlatkogalev.domain.user.service

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AnonymousAuthTest : UserAuthServiceTestBase() {
    @Test
    fun authenticateAnonymous_blankInstallationId_returnsFailure() {
        val result = runBlocking { service.authenticateAnonymous(" ") }

        assertEquals("installationId is required", assertFailure(result).reason)
    }

    @Test
    fun authenticateAnonymous_firstLaunch_createsAnonymousUser() {
        val session = assertSuccess(runBlocking { service.authenticateAnonymous("install-1") }).value

        assertTrue(session.accessToken.isNotBlank())
        assertTrue(session.refreshToken.isNotBlank())
        assertTrue(session.user.isAnonymous)
        assertEquals(null, session.user.email)
    }

    @Test
    fun authenticateAnonymous_repeatedLaunch_reusesSameUser() {
        val first = assertSuccess(runBlocking { service.authenticateAnonymous("install-2") }).value
        val second = assertSuccess(runBlocking { service.authenticateAnonymous("install-2") }).value

        assertEquals(first.user.id, second.user.id)
        assertNotEquals(first.refreshToken, second.refreshToken)
    }

    @Test
    fun signup_upgradeAnonymous_keepsUserIdAndDisablesAnonymous() {
        val anonymous = assertSuccess(runBlocking { service.authenticateAnonymous("install-3") }).value
        val upgraded = assertSuccess(
            runBlocking { service.signup("upgrade@example.com", TestFixtures.VALID_PASSWORD, "Test", "User", anonymous.user.id) },
        ).value

        assertEquals(anonymous.user.id, upgraded.user.id)
        assertFalse(upgraded.user.isAnonymous)
        assertNotNull(upgraded.user.upgradedAt)
    }

    @Test
    fun signup_requiresAnonymousUser() {
        val registered = assertSuccess(
            runBlocking { service.register("registered@example.com", TestFixtures.VALID_PASSWORD, "John", "Doe") },
        ).value

        val result = runBlocking { service.signup("direct@example.com", TestFixtures.VALID_PASSWORD, "Test", "User", registered.id) }

        assertEquals("Signup upgrade requires anonymous account", assertFailure(result).reason)
    }

    @Test
    fun signup_duplicateEmail_returnsFailure() {
        val anonymous = assertSuccess(runBlocking { service.authenticateAnonymous("install-4") }).value
        assertSuccess(runBlocking { service.signup("dup@example.com", TestFixtures.VALID_PASSWORD, "Test", "User", anonymous.user.id) })

        val anotherAnonymous = assertSuccess(runBlocking { service.authenticateAnonymous("install-5") }).value

        val result = runBlocking { service.signup("dup@example.com", TestFixtures.VALID_PASSWORD, "Test", "User", anotherAnonymous.user.id) }

        assertEquals("Email already registered", assertFailure(result).reason)
    }
}
