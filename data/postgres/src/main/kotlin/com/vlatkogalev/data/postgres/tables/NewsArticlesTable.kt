package com.vlatkogalev.data.postgres.tables

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object NewsArticlesTable : Table("news_articles") {
    val id = javaUUID("id")
    val guid = text("guid")
    val title = text("title")
    val link = text("link")
    val description = text("description")
    val content = text("content")
    val author = text("author").nullable()
    val imageUrl = text("image_url").nullable()
    val publishedAt = timestampWithTimeZone("published_at")
    val fetchedAt = timestampWithTimeZone("fetched_at")

    override val primaryKey = PrimaryKey(id)
}
