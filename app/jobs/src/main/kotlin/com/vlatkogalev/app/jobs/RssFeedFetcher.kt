package com.vlatkogalev.app.jobs

import com.vlatkogalev.domain.news.model.NewsArticle
import com.vlatkogalev.domain.news.repository.NewsRepository
import org.jsoup.Jsoup
import org.jsoup.safety.Safelist
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import java.util.UUID

class RssFeedFetcher(
    private val newsRepository: NewsRepository,
    private val feedUrl: String = "https://coinweek.com/feed/",
    private val userAgent: String = "MintMind-RssBot/1.0",
) {
    private val excludedCategories = setOf(
        "dealers and companies",
        "bullion & precious metals",
        "paper money",
    )

    private val rssDateFormat = DateTimeFormatter.ofPattern(
        "EEE, dd MMM yyyy HH:mm:ss Z",
        Locale.ENGLISH,
    )

    suspend fun run() {
        try {
            val xml = Jsoup.connect(feedUrl)
                .userAgent(userAgent)
                .ignoreContentType(true)
                .parser(org.jsoup.parser.Parser.xmlParser())
                .get()

            val items = xml.select("item").mapNotNull { item ->
                try {
                    val categories = item.select("category").map { it.text().lowercase() }
                    if (categories.any { it in excludedCategories }) return@mapNotNull null

                    val guid = item.select("guid").text().ifBlank { item.select("link").text() }
                    if (guid.isBlank()) return@mapNotNull null

                    val descriptionHtml = item.select("description").text().ifBlank { "" }
                    val contentEncoded = item.select("content|encoded").text()
                    val contentHtml = if (contentEncoded.isNotBlank()) {
                        sanitizeHtml(contentEncoded)
                    } else {
                        sanitizeHtml(item.select("description").html())
                    }

                    val publishedAt = try {
                        ZonedDateTime.parse(
                            item.select("pubDate").text(),
                            rssDateFormat,
                        ).toInstant()
                    } catch (e: DateTimeParseException) {
                        Instant.now()
                    }

                    NewsArticle(
                        id = UUID.randomUUID(),
                        guid = guid,
                        title = item.select("title").text(),
                        link = item.select("link").text(),
                        description = Jsoup.parse(descriptionHtml).text(),
                        content = contentHtml,
                        author = item.select("dc|creator").text().ifBlank { null },
                        imageUrl = extractImageUrl(contentHtml),
                        publishedAt = publishedAt,
                        fetchedAt = Instant.now(),
                    )
                } catch (e: Exception) {
                    null
                }
            }

            val incomingGuids = items.map { it.guid }.toSet()
            if (incomingGuids.isEmpty()) return

            val existing = newsRepository.existingGuids(incomingGuids)
            val newArticles = items.filter { it.guid !in existing }

            if (newArticles.isNotEmpty()) {
                newsRepository.saveAll(newArticles)
            }

            println("News fetch: ${newArticles.size} inserted, ${items.size - newArticles.size} skipped")
        } catch (e: Exception) {
            println("News fetch failed: ${e.message}")
        }
    }

    private fun sanitizeHtml(html: String): String {
        val safelist = Safelist.relaxed()
            .removeTags("script", "iframe", "style", "form", "input", "button")
            .removeAttributes("style", "onclick", "onload", "onerror")
            .addTags("img[src,alt,width,height]", "a[href]", "p[class]", "h1[class]", "h2[class]", "h3[class]")

        return Jsoup.clean(html, safelist)
    }

    private fun extractImageUrl(html: String): String? {
        if (html.isBlank()) return null
        return try {
            Jsoup.parse(html).select("img").first()?.attr("src")?.ifBlank { null }
        } catch (e: Exception) {
            null
        }
    }
}
