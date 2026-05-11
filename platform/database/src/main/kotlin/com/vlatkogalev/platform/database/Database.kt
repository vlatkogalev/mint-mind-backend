package com.vlatkogalev.platform.database

import com.vlatkogalev.platform.core.config.DatabaseConfig
import com.vlatkogalev.platform.core.config.loadDatabaseConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import javax.sql.DataSource

fun createDataSource(config: DatabaseConfig = loadDatabaseConfig()): DataSource {
    val hikariConfig = HikariConfig().apply {
        jdbcUrl = config.url
        driverClassName = when {
            config.url.startsWith("jdbc:postgresql:", ignoreCase = true) -> "org.postgresql.Driver"
            config.url.startsWith("jdbc:h2:", ignoreCase = true) -> "org.h2.Driver"
            else -> null
        }
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
