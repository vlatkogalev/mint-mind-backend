package com.vlatkogalev.platform.database

import com.vlatkogalev.platform.core.config.DatabaseConfig
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.testcontainers.containers.PostgreSQLContainer
import javax.sql.DataSource

object PostgresTestContainer {
    private val container: PostgreSQLContainer<*> by lazy {
        PostgreSQLContainer("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .also { it.start() }
    }

    private val config by lazy {
        DatabaseConfig(
            url = container.jdbcUrl,
            user = container.username,
            password = container.password,
        )
    }

    val dataSource: DataSource by lazy {
        val dataSource = createDataSource(config)
        runMigrations(dataSource)
        dataSource
    }

    val r2dbcDatabase: R2dbcDatabase by lazy {
        runMigrations(dataSource)
        connectDatabase(config)
    }

    val jdbcUrl: String get() = container.jdbcUrl
    val r2dbcUrl: String get() = config.r2dbcUrl
    val username: String get() = container.username
    val password: String get() = container.password
}
