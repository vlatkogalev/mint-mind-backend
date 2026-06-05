package com.vlatkogalev.platform.database

import com.vlatkogalev.platform.core.config.DatabaseConfig
import com.vlatkogalev.platform.core.config.loadDatabaseConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
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

fun connectDatabase(config: DatabaseConfig = loadDatabaseConfig()): R2dbcDatabase =
    R2dbcDatabase.connect(
        url = config.r2dbcUrl,
        driver = "postgresql",
        user = config.user,
        password = config.password,
    )

suspend fun <T> dbQuery(database: R2dbcDatabase, block: suspend () -> T): T =
    suspendTransaction(db = database) { block() }
