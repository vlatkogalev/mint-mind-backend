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
        driverClassName = "org.postgresql.Driver"
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
