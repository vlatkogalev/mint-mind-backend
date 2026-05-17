package com.vlatkogalev.app.jobs

import com.vlatkogalev.domain.news.model.NewsArticle
import com.vlatkogalev.domain.news.repository.NewsRepository
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.parser.Parser
import org.jsoup.safety.Safelist
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

class RssFeedFetcher(
    private val newsRepository: NewsRepository,
    private val feedUrl: String = "https://coinweek.com/feed/",
) {
    private val log = LoggerFactory.getLogger(RssFeedFetcher::class.java)
    private val http = HttpClient.newHttpClient()

    private val rssDateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH)

    /**
     * Categories to silently drop. Comparison is case-insensitive and
     * trims whitespace so minor feed inconsistencies don't slip through.
     */
    private val excludedCategories = setOf(
        "dealers and companies",
        "bullion & precious metals",
        "paper money",
    )

    /**
     * Tags allowed in the stored HTML content.
     * Strips scripts, iframes, inline styles, and anything that could
     * cause layout or security issues inside a mobile WebView.
     */
    private val contentSafelist: Safelist = Safelist.relaxed()
        .removeTags("script", "iframe", "style", "form", "input", "button")
        .removeAttributes(":all", "style", "onclick", "onload", "onerror")
        .addAttributes("img", "src", "alt", "width", "height")
        .addAttributes("a", "href")
        .addAttributes("p", "class")
        .addAttributes("h1", "class")
        .addAttributes("h2", "class")
        .addAttributes("h3", "class")

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
            log.info("RssFeedFetcher: no items after filtering")
            return
        }

        val incomingGuids = items.map { it.guid }.toSet()
        val existingGuids = newsRepository.existingGuids(incomingGuids)
        val newItems = items.filter { it.guid !in existingGuids }

        if (newItems.isEmpty()) {
            log.info("RssFeedFetcher: all {} items already present", items.size)
            return
        }

        newsRepository.saveAll(newItems)
        log.info(
            "RssFeedFetcher: inserted {} new articles ({} skipped as duplicates)",
            newItems.size, existingGuids.size,
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
        val doc: Document = Jsoup.parse(xml, feedUrl, Parser.xmlParser())
        val now = Instant.now()

        return doc.select("item").mapNotNull { item ->
            try {
                val categories = item.select("category")
                    .map { it.text().trim().lowercase() }

                if (categories.any { it in excludedCategories }) {
                    log.debug(
                        "RssFeedFetcher: skipping '{}' (excluded category: {})",
                        item.selectFirst("title")?.text(),
                        categories.filter { it in excludedCategories },
                    )
                    return@mapNotNull null
                }

                val guid = item.selectFirst("guid")?.text()?.trim()
                    ?: item.selectFirst("link")?.text()?.trim()
                    ?: return@mapNotNull null

                val title = item.selectFirst("title")?.text()?.trim() ?: ""
                val link = item.selectFirst("link")?.text()?.trim() ?: ""
                val author = item.selectFirst("dc|creator")?.text()?.trim()

                val descriptionRaw = item.selectFirst("description")?.text() ?: ""
                val description = Jsoup.parse(descriptionRaw).text().trim()

                val contentRaw = item.selectFirst("content|encoded")?.text()
                    ?: item.selectFirst("description")?.text()
                    ?: ""
                val contentHtml = sanitizeHtml(contentRaw)

                val imageUrl = Jsoup.parse(contentRaw)
                    .selectFirst("img[src]")
                    ?.attr("src")

                val pubDateRaw = item.selectFirst("pubDate")?.text()?.trim()
                val publishedAt = pubDateRaw?.let { parseRssDate(it) } ?: now

                NewsArticle(
                    id = UUID.randomUUID(),
                    guid = guid,
                    title = title,
                    link = link,
                    description = description,
                    content = contentHtml,
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

    /**
     * Sanitizes raw HTML from the RSS feed:
     * - Removes scripts, iframes, inline styles, forms
     * - Keeps semantic structure: headings, paragraphs, lists, images, links
     * - Rewrites relative URLs to absolute using the feed's base URL
     */
    private fun sanitizeHtml(rawHtml: String): String =
        Jsoup.clean(rawHtml, feedUrl, contentSafelist)

    private fun parseRssDate(raw: String): Instant =
        ZonedDateTime.parse(raw.trim(), rssDateFormatter).toInstant()
}