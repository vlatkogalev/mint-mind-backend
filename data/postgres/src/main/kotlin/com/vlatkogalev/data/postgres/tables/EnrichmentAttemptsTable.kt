package com.vlatkogalev.data.postgres.tables

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object EnrichmentAttemptsTable : Table("enrichment_attempts") {
    val fingerprintHash = text("fingerprint_hash")
    val retrievalKey = text("retrieval_key")
    val lastAttemptAt = timestampWithTimeZone("last_attempt_at")
    val lastResult = varchar("last_result", 16)

    override val primaryKey = PrimaryKey(fingerprintHash)
}
