package com.vlatkogalev.platform.database

import com.vlatkogalev.platform.core.config.DatabaseConfig
import com.vlatkogalev.platform.core.config.loadDatabaseConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import java.sql.Connection
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
    val vendor = dataSource.connection.use { connection ->
        when (connection.metaData.databaseProductName.lowercase()) {
            "h2" -> "h2"
            else -> "postgresql"
        }
    }

    Flyway.configure()
        .dataSource(dataSource)
        .locations("classpath:db/migration/$vendor")
        .load()
        .migrate()
}

fun <T> DataSource.withTransaction(block: (Connection) -> T): T =
    connection.use { conn ->
        val originalAutoCommit = conn.autoCommit
        conn.autoCommit = false
        try {
            val result = block(conn)
            conn.commit()
            result
        } catch (ex: Exception) {
            conn.rollback()
            throw ex
        } finally {
            conn.autoCommit = originalAutoCommit
        }
    }
