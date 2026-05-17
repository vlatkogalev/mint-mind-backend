package com.vlatkogalev.app.jobs

import com.vlatkogalev.domain.news.model.NewsArticle
import com.vlatkogalev.domain.news.repository.NewsRepository
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.parser.Parser
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

/**
 * Fetches the CoinWeek RSS feed (or any RSS 2.0 feed) and persists new
 * articles to the database.  Duplicate detection is done by <guid>.
 *
 * Call [run] from a scheduler (e.g. java.util.concurrent.ScheduledExecutorService
 * or a Ktor background coroutine).
 */
class RssFeedFetcher(
    private val newsRepository: NewsRepository,
    private val feedUrl: String = "https://coinweek.com/feed/",
) {
    private val log = LoggerFactory.getLogger(RssFeedFetcher::class.java)
    private val http = HttpClient.newHttpClient()

    // RFC 1123 / RFC 2822 date format used by RSS feeds
    private val rssDateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH)

    fun run() {
        log.info("RssFeedFetcher: fetching {}", feedUrl)

        val xml = try {
            fetchXml()
        } catch (ex: Exception) {
            log.error("RssFeedFetcher: failed to download feed", ex)
            return
        }

        val items = try {
            parseItems(xml)
        } catch (ex: Exception) {
            log.error("RssFeedFetcher: failed to parse feed", ex)
            return
        }

        if (items.isEmpty()) {
            log.info("RssFeedFetcher: no items found in feed")
            return
        }

        // Dedup – only insert items whose guid isn't in the DB yet
        val incomingGuids = items.map { it.guid }.toSet()
        val existingGuids = newsRepository.existingGuids(incomingGuids)
        val newItems = items.filter { it.guid !in existingGuids }

        if (newItems.isEmpty()) {
            log.info("RssFeedFetcher: all {} items already present, nothing to insert", items.size)
            return
        }

        newsRepository.saveAll(newItems)
        log.info(
            "RssFeedFetcher: inserted {} new articles ({} already existed)",
            newItems.size,
            existingGuids.size,
        )
    }

    private fun fetchXml(): String {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(feedUrl))
            .header("User-Agent", "MintMind-RssBot/1.0")
            .GET()
            .build()
        val response = http.send(request, HttpResponse.BodyHandlers.ofString())
        check(response.statusCode() in 200..299) {
            "Unexpected HTTP ${response.statusCode()} from $feedUrl"
        }
        return response.body()
    }

    private fun parseItems(xml: String): List<NewsArticle> {
        // Use Jsoup's XML parser so HTML entities in CDATA are handled correctly
        val doc: Document = Jsoup.parse(xml, feedUrl, Parser.xmlParser())
        val now = Instant.now()

        return doc.select("item").mapNotNull { item ->
            try {
                val guid = item.selectFirst("guid")?.text()?.trim()
                    ?: item.selectFirst("link")?.text()?.trim()
                    ?: return@mapNotNull null   // can't dedup without a guid

                val title = item.selectFirst("title")?.text()?.trim() ?: ""
                val link = item.selectFirst("link")?.text()?.trim() ?: ""
                val author = item.selectFirst("dc|creator")?.text()?.trim()

                // <description> is a plain-text excerpt; strip any residual tags
                val descriptionHtml = item.selectFirst("description")?.text() ?: ""
                val description = Jsoup.parse(descriptionHtml).text().trim()

                // content:encoded is the full HTML body
                val contentHtml = item.selectFirst("content|encoded")?.text()
                    ?: item.selectFirst("description")?.text()
                    ?: ""
                val contentDoc = Jsoup.parse(contentHtml)
                val imageUrl = contentDoc.selectFirst("img[src]")?.attr("src")
                val content = contentDoc.text().trim()

                val pubDateRaw = item.selectFirst("pubDate")?.text()?.trim()
                val publishedAt = pubDateRaw?.let { parseRssDate(it) } ?: now

                NewsArticle(
                    id = UUID.randomUUID(),
                    guid = guid,
                    title = title,
                    link = link,
                    description = description,
                    content = content,
                    author = author,
                    imageUrl = imageUrl,
                    publishedAt = publishedAt,
                    fetchedAt = now,
                )
            } catch (ex: Exception) {
                log.warn("RssFeedFetcher: skipping item due to parse error", ex)
                null
            }
        }
    }

    private fun parseRssDate(raw: String): Instant =
        ZonedDateTime.parse(raw.trim(), rssDateFormatter).toInstant()
}
