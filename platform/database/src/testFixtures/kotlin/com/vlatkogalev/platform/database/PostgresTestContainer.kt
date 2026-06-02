package com.vlatkogalev.platform.database

import com.vlatkogalev.platform.core.config.DatabaseConfig
import org.testcontainers.containers.PostgreSQLContainer
import javax.sql.DataSource

/**
 * Shared PostgreSQL container for integration tests in a single test worker.
 */
object PostgresTestContainer {
    private val container: PostgreSQLContainer<*> by lazy {
        PostgreSQLContainer("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .also { it.start() }
    }

    val dataSource: DataSource by lazy {
        val config = DatabaseConfig(
            url = container.jdbcUrl,
            user = container.username,
            password = container.password,
        )
        val dataSource = createDataSource(config)
        runMigrations(dataSource)
        dataSource
    }
}