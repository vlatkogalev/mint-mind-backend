package com.vlatkogalev.platform.database

import com.vlatkogalev.platform.core.config.DatabaseConfig
import com.vlatkogalev.platform.core.config.loadDatabaseConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.Application
import org.flywaydb.core.Flyway
import javax.sql.DataSource

fun Application.configureDatabase(config: DatabaseConfig = loadDatabaseConfig()): DataSource {
    val dataSource = createDataSource(config)
    runMigrations(dataSource)
    return dataSource
}

fun createDataSource(config: DatabaseConfig = loadDatabaseConfig()): DataSource {
    val hikariConfig = HikariConfig().apply {
        jdbcUrl = config.url
        username = config.user
        password = config.password
        maximumPoolSize = 8
        isAutoCommit = true
    }

    return HikariDataSource(hikariConfig)
}

fun runMigrations(dataSource: DataSource) {
    Flyway.configure()
        .dataSource(dataSource)
        .locations("classpath:db/migration")
        .load()
        .migrate()
}
