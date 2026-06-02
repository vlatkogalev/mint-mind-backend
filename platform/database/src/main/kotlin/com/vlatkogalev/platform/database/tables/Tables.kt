package com.vlatkogalev.platform.database.tables

import org.jetbrains.exposed.sql.CustomFunction
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.UUIDColumnType
import org.jetbrains.exposed.sql.javatime.CurrentTimestampWithTimeZone
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

object UsersTable : Table("users") {
    val id = uuid("id")
    val email = varchar("email", 320).nullable()
    val passwordHash = varchar("password_hash", 512).nullable()
    val emailVerified = bool("email_verified").default(false)
    val verificationToken = varchar("verification_token", 128).nullable()
    val verificationEmailSentAt = timestampWithTimeZone("verification_email_sent_at").nullable()
    val refreshTokenHash = varchar("refresh_token_hash", 512).nullable()
    val isAnonymous = bool("is_anonymous").default(true)
    val upgradedAt = timestampWithTimeZone("upgraded_at").nullable()
    val createdAt = timestampWithTimeZone("created_at").defaultExpression(CurrentTimestampWithTimeZone)
    val updatedAt = timestampWithTimeZone("updated_at").defaultExpression(CurrentTimestampWithTimeZone)

    override val primaryKey = PrimaryKey(id)
}

object ProfilesTable : Table("profiles") {
    val id = uuid("id")
    val userId = uuid("user_id")
    val firstName = varchar("first_name", 255)
    val lastName = varchar("last_name", 255)
    val avatarUrl = varchar("avatar_url", 2048).nullable()
    val updatedAt = timestampWithTimeZone("updated_at").defaultExpression(CurrentTimestampWithTimeZone)

    override val primaryKey = PrimaryKey(id)
}

object PasswordResetTokensTable : Table("password_reset_tokens") {
    val userId = uuid("user_id")
    val token = varchar("token", 128)
    val expiresAt = timestampWithTimeZone("expires_at")
    val createdAt = timestampWithTimeZone("created_at").defaultExpression(CurrentTimestampWithTimeZone)

    override val primaryKey = PrimaryKey(userId)
}

object UserAuthIdentitiesTable : Table("user_auth_identities") {
    val id = uuid("id")
    val userId = uuid("user_id")
    val authType = varchar("auth_type", 32)
    val email = varchar("email", 320).nullable()
    val passwordHash = varchar("password_hash", 512).nullable()
    val createdAt = timestampWithTimeZone("created_at").defaultExpression(CurrentTimestampWithTimeZone)

    override val primaryKey = PrimaryKey(id)
}

object AnonymousInstallationsTable : Table("anonymous_installations") {
    val installationId = varchar("installation_id", 255)
    val userId = uuid("user_id")
    val createdAt = timestampWithTimeZone("created_at").defaultExpression(CurrentTimestampWithTimeZone)
    val lastSeenAt = timestampWithTimeZone("last_seen_at").defaultExpression(CurrentTimestampWithTimeZone)

    override val primaryKey = PrimaryKey(installationId)
}

object SubscriptionsTable : Table("subscriptions") {
    val id = uuid("id")
    val userId = uuid("user_id")
    val rcCustomerId = varchar("rc_customer_id", 255)
    val plan = varchar("plan", 32).default("free")
    val status = varchar("status", 32).default("active")
    val expiresAt = timestampWithTimeZone("expires_at").nullable()
    val updatedAt = timestampWithTimeZone("updated_at").defaultExpression(CurrentTimestampWithTimeZone)

    override val primaryKey = PrimaryKey(id)
}

object CatalogCoinsTable : Table("catalog_coins") {
    val id = uuid("id").defaultExpression(CustomFunction("gen_random_uuid", UUIDColumnType()))
    val countryOrIssuer = text("country_or_issuer").nullable()
    val denomination = text("denomination").nullable()
    val seriesName = text("series_name").nullable()
    val title = text("title").nullable()
    val year = integer("year").nullable()
    val mintMark = text("mint_mark").nullable()
    val enrichedAt = timestampWithTimeZone("enriched_at").nullable()
    val lastEnrichmentAttemptAt = timestampWithTimeZone("last_enrichment_attempt_at").nullable()
    val lastEnrichmentFailedAt = timestampWithTimeZone("last_enrichment_failed_at").nullable()
    val lastEnrichmentError = text("last_enrichment_error").nullable()
    val createdAt = timestampWithTimeZone("created_at").defaultExpression(CurrentTimestampWithTimeZone)
    val updatedAt = timestampWithTimeZone("updated_at").defaultExpression(CurrentTimestampWithTimeZone)

    override val primaryKey = PrimaryKey(id)
}

object CoinSetsTable : Table("coin_sets") {
    val id = uuid("id")
    val userId = uuid("user_id")
    val name = varchar("name", 255)
    val description = text("description").nullable()
    val createdAt = timestampWithTimeZone("created_at").defaultExpression(CurrentTimestampWithTimeZone)

    override val primaryKey = PrimaryKey(id)
}

object CoinsTable : Table("coins") {
    val id = uuid("id")
    val userId = uuid("user_id")
    val obverseKey = text("obverse_key")
    val reverseKey = text("reverse_key")
    val notes = text("notes").nullable()
    val createdAt = timestampWithTimeZone("created_at").defaultExpression(CurrentTimestampWithTimeZone)
    val overallConfidence = text("overall_confidence")
    val countryOrIssuer = text("country_or_issuer").nullable()
    val denomination = text("denomination").nullable()
    val seriesName = text("series_name").nullable()
    val year = integer("year").nullable()
    val mintMark = text("mint_mark").nullable()
    val metalComposition = text("metal_composition").nullable()
    val estimatedGrade = text("estimated_grade").nullable()
    val estimatedGradeValue = text("estimated_grade_value").nullable()
    val rarityQualitative = text("rarity_qualitative").nullable()
    val valueLow = decimal("value_low", 15, 2).nullable()
    val valueHigh = decimal("value_high", 15, 2).nullable()
    val obverseDescription = text("obverse_description").nullable()
    val reverseDescription = text("reverse_description").nullable()
    val historicalContext = text("historical_context").nullable()
    val rawJson = text("raw_json")
    val mintage = long("mintage").nullable()
    val setId = uuid("set_id").nullable()
    val catalogCoinId = uuid("catalog_coin_id").nullable()

    override val primaryKey = PrimaryKey(id)
}

object CoinCatalogueNumbersTable : Table("coin_catalogue_numbers") {
    val id = uuid("id").defaultExpression(CustomFunction("gen_random_uuid", UUIDColumnType()))
    val coinId = uuid("coin_id")
    val catalogueName = text("catalogue_name")
    val number = text("number").nullable()
    val confidence = text("confidence")

    override val primaryKey = PrimaryKey(id)
}

object ExternalCoinReferencesTable : Table("external_coin_references") {
    val id = uuid("id").defaultExpression(CustomFunction("gen_random_uuid", UUIDColumnType()))
    val catalogCoinId = uuid("catalog_coin_id")
    val provider = varchar("provider", 64)
    val externalId = text("external_id")
    val externalUrl = text("external_url").nullable()
    val lastSyncedAt = timestampWithTimeZone("last_synced_at").nullable()
    val syncStatus = text("sync_status").nullable()
    val syncError = text("sync_error").nullable()
    val createdAt = timestampWithTimeZone("created_at").defaultExpression(CurrentTimestampWithTimeZone)

    override val primaryKey = PrimaryKey(id)
}

object MarketplaceListingsTable : Table("marketplace_listings") {
    val id = uuid("id")
    val ebayItemId = text("ebay_item_id")
    val title = text("title")
    val price = text("price")
    val currency = varchar("currency", 10).default("USD")
    val condition = text("condition").nullable()
    val listingUrl = text("listing_url")
    val imageUrl = text("image_url").nullable()
    val buyingOptions = array<String>("buying_options").default(emptyList())
    val expiresAt = timestampWithTimeZone("expires_at").nullable()
    val lastSeenAt = timestampWithTimeZone("last_seen_at").defaultExpression(CurrentTimestampWithTimeZone)

    override val primaryKey = PrimaryKey(id)
}

object NewsArticlesTable : Table("news_articles") {
    val id = uuid("id")
    val guid = text("guid")
    val title = text("title")
    val link = text("link")
    val description = text("description").default("")
    val content = text("content").default("")
    val author = text("author").nullable()
    val imageUrl = text("image_url").nullable()
    val publishedAt = timestampWithTimeZone("published_at")
    val fetchedAt = timestampWithTimeZone("fetched_at").defaultExpression(CurrentTimestampWithTimeZone)

    override val primaryKey = PrimaryKey(id)
}
