package com.vlatkogalev.data.ebay

import com.vlatkogalev.platform.core.config.EbayConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference

/**
 * Thread-safe shared token provider for eBay API access.
 */
class EbayTokenProvider(
    private val config: EbayConfig,
) {
    private val cachedToken = AtomicReference<CachedToken?>(null)
    private val httpClient = HttpClient.newHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    fun getAccessToken(): String {
        val existing = cachedToken.get()
        if (existing != null && !existing.isExpired()) {
            return existing.token
        }

        val fresh = fetchNewToken()
        cachedToken.set(fresh)
        return fresh.token
    }

    private fun fetchNewToken(): CachedToken {
        val body = "grant_type=client_credentials&scope=${
            URLEncoder.encode(config.oauthScope, Charsets.UTF_8)
        }"

        val request = HttpRequest.newBuilder()
            .uri(URI.create(config.oauthEndpoint))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Authorization", "Basic ${config.authHeaderValue}")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        check(response.statusCode() in 200..299) {
            "eBay token request failed: ${response.statusCode()} ${response.body()}"
        }

        val tokenResponse = json.decodeFromString<TokenResponse>(response.body())
        return CachedToken(
            token = tokenResponse.accessToken,
            expiresAt = Instant.now().plusSeconds(tokenResponse.expiresIn - 60),
        )
    }

    data class CachedToken(
        val token: String,
        val expiresAt: Instant,
    ) {
        fun isExpired(): Boolean = Instant.now().isAfter(expiresAt)
    }

    @Serializable
    private data class TokenResponse(
        @SerialName("access_token") val accessToken: String,
        @SerialName("expires_in") val expiresIn: Long,
    )
}
