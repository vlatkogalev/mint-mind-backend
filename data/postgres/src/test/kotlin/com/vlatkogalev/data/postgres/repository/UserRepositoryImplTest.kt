package com.vlatkogalev.data.postgres.repository

import com.vlatkogalev.data.postgres.daos.UserQueries
import com.vlatkogalev.platform.database.PostgresTestContainer
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class UserRepositoryImplTest {
    private val dataSource = PostgresTestContainer.dataSource
    private val repo = UserRepositoryImpl(UserQueries(dataSource), dataSource)

    @BeforeTest
    fun cleanUp() {
        dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate("DELETE FROM coin_catalogue_numbers")
                statement.executeUpdate("DELETE FROM coins")
                statement.executeUpdate("DELETE FROM coin_sets")
                statement.executeUpdate("DELETE FROM marketplace_listings")
                statement.executeUpdate("DELETE FROM news_articles")
                statement.executeUpdate("DELETE FROM user_auth_identities")
                statement.executeUpdate("DELETE FROM password_reset_tokens")
                statement.executeUpdate("DELETE FROM anonymous_installations")
                statement.executeUpdate("DELETE FROM subscriptions")
                statement.executeUpdate("DELETE FROM profiles")
                statement.executeUpdate("DELETE FROM users")
            }
        }
    }

    @Test
    fun `create and findById round-trip`() {
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
