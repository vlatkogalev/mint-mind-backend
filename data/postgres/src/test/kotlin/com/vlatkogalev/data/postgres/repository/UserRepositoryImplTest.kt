package com.vlatkogalev.data.postgres.repository

import com.vlatkogalev.platform.database.PostgresTestContainer
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class UserRepositoryImplTest {
    private val database = PostgresTestContainer.r2dbcDatabase
    private val repo = UserRepositoryImpl(database)

    @BeforeTest
    fun cleanUp() {
        PostgresTestContainer.dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate(
                    """
                    TRUNCATE TABLE
                        user_auth_identities,
                        password_reset_tokens,
                        anonymous_installations,
                        subscriptions,
                        profiles,
                        users
                    CASCADE
                    """.trimIndent()
                )
            }
        }
    }

    @Test
    fun `create and findById round-trip`() = runTest {
        val created = repo.create(
            email = "alice@example.com",
            firstName = "Alice",
            lastName = "Smith",
            passwordHash = "hashed-password",
            verificationToken = "token-123",
        )

        val found = repo.findById(created.id)
        assertNotNull(found)
        assertFalse(found.emailVerified)
        assertNotNull(found.profile)
        assertNull(found.upgradedAt)
    }
}
