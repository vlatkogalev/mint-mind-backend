package com.vlatkogalev.data.postgres.tables

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object CatalogCoinsTable : Table("catalog_coins") {
    val id = javaUUID("id")
    val countryOrIssuer = text("country_or_issuer").nullable()
    val denomination = text("denomination").nullable()
    val seriesName = text("series_name").nullable()
    val title = text("title").nullable()
    val year = integer("year").nullable()
    val mintMark = text("mint_mark").nullable()
    val composition = text("composition").nullable()
    val weightGrams = double("weight_grams").nullable()
    val diameterMm = double("diameter_mm").nullable()
    val obverseDescription = text("obverse_description").nullable()
    val reverseDescription = text("reverse_description").nullable()
    val historicalContext = text("historical_context").nullable()
    val thumbnailUrl = text("thumbnail_url").nullable()
    val numistaUrl = text("numista_url").nullable()
    val enrichedAt = timestampWithTimeZone("enriched_at").nullable()
    val lastEnrichmentAttemptAt = timestampWithTimeZone("last_enrichment_attempt_at").nullable()
    val lastEnrichmentFailedAt = timestampWithTimeZone("last_enrichment_failed_at").nullable()
    val lastEnrichmentError = text("last_enrichment_error").nullable()
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")

    override val primaryKey = PrimaryKey(id)
}
