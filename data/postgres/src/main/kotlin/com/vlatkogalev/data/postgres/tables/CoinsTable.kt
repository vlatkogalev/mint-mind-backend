package com.vlatkogalev.data.postgres.tables

import com.vlatkogalev.data.postgres.columns.PostgresTextArrayColumnType
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
    val era = text("era").nullable()
    // Identification confidence
    val confidenceCountry = text("confidence_country").nullable()
    val confidenceDenomination = text("confidence_denomination").nullable()
    val confidenceSeries = text("confidence_series").nullable()
    val confidenceYear = text("confidence_year").nullable()
    val confidenceEra = text("confidence_era").nullable()
    val mintMark = text("mint_mark").nullable()
    // Mint mark detail
    val mintMarkStatus = text("mint_mark_status").nullable()
    val mintMarkConfidence = text("mint_mark_confidence").nullable()
    val metalComposition = text("metal_composition").nullable()
    val gradeName = text("grade_name").nullable()
    val gradeAbbreviation = text("grade_abbreviation").nullable()
    val gradeNumeric = integer("grade_numeric").nullable()
    val gradeConfidence = text("grade_confidence").nullable()
    val rarityQualitative = text("rarity_qualitative").nullable()
    val rarityScore = double("rarity_score").nullable()
    val valueLow = double("value_low").nullable()
    val valueHigh = double("value_high").nullable()
    val valueCurrency = text("value_currency").nullable()
    val mintage = long("mintage").nullable()
    val obverseDescription = text("obverse_description").nullable()
    val reverseDescription = text("reverse_description").nullable()
    // Specifications
    val weightGrams = double("weight_grams").nullable()
    val diameterMm = double("diameter_mm").nullable()
    val thicknessMm = double("thickness_mm").nullable()
    val edge = text("edge").nullable()
    val designerObverse = text("designer_obverse").nullable()
    val designerReverse = text("designer_reverse").nullable()
    // Condition
    val positiveFeatures = registerColumn("positive_features", PostgresTextArrayColumnType())
    val negativeFeatures = registerColumn("negative_features", PostgresTextArrayColumnType())
    // Market
    val supplySummary = text("supply_summary").nullable()
    val demandSummary = text("demand_summary").nullable()
    val valueDisclaimer = text("value_disclaimer").nullable()
    // Design lettering
    val obverseLettering = text("obverse_lettering").nullable()
    val reverseLettering = text("reverse_lettering").nullable()
    val analysisNotes = text("analysis_notes").nullable()
    val historicalContext = text("historical_context").nullable()
    // Image analysis
    val obverseVisible = bool("obverse_visible").nullable()
    val reverseVisible = bool("reverse_visible").nullable()
    val imageFocus = text("image_focus").nullable()
    val imageLighting = text("image_lighting").nullable()
    val imageResolution = text("image_resolution").nullable()
    val imageCropping = text("image_cropping").nullable()
    val imageIssues = registerColumn("image_issues", PostgresTextArrayColumnType())
    val rawJson = text("raw_json")
    val createdAt = timestampWithTimeZone("created_at")

    override val primaryKey = PrimaryKey(id)
}
