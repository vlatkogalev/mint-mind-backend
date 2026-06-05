package com.vlatkogalev.data.postgres.tables

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object CoinsTable : Table("coins") {
    val id = javaUUID("id")
    val userId = javaUUID("user_id")
    val obverseKey = text("obverse_key")
    val reverseKey = text("reverse_key")
    val setId = javaUUID("set_id").nullable()
    val catalogCoinId = javaUUID("catalog_coin_id").nullable()
    val notes = text("notes").nullable()
    val overallConfidence = varchar("overall_confidence", 16)
    val countryOrIssuer = text("country_or_issuer").nullable()
    val denomination = text("denomination").nullable()
    val seriesName = text("series_name").nullable()
    val year = integer("year").nullable()
    val mintMark = text("mint_mark").nullable()
    val metalComposition = text("metal_composition").nullable()
    val estimatedGrade = text("estimated_grade").nullable()
    val estimatedGradeValue = text("estimated_grade_value").nullable()
    val rarityQualitative = text("rarity_qualitative").nullable()
    val valueLow = double("value_low").nullable()
    val valueHigh = double("value_high").nullable()
    val mintage = long("mintage").nullable()
    val obverseDescription = text("obverse_description").nullable()
    val reverseDescription = text("reverse_description").nullable()
    val historicalContext = text("historical_context").nullable()
    val rawJson = text("raw_json")
    val createdAt = timestampWithTimeZone("created_at")

    override val primaryKey = PrimaryKey(id)
}
