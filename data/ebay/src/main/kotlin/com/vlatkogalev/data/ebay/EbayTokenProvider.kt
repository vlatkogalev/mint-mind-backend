package com.vlatkogalev.data.ebay

import com.vlatkogalev.platform.core.config.EbayConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant

private data class CachedToken(
    val token: String,
    val expiresAt: Instant,
)

class EbayTokenProvider(
    private val config: EbayConfig,
) {
    private val mutex = Mutex()
    private var cachedToken: CachedToken? = null

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    suspend fun getAccessToken(): String = mutex.withLock {
        cachedToken?.let {
            if (Instant.now().isBefore(it.expiresAt)) return@withLock it.token
        }

        val response: EbayTokenResponse =
            client.post(config.oauthEndpoint) {
                header("Authorization", "Basic ${config.authHeaderValue}")
                header("Content-Type", "application/x-www-form-urlencoded")
                setBody(
                    FormDataContent(
                        io.ktor.http.Parameters.build {
                            append("grant_type", "client_credentials")
                            append("scope", config.oauthScope)
                        }
                    )
                )
            }.body()

        val expiresAt = Instant.now().plusSeconds(response.expiresIn - 60)
        cachedToken = CachedToken(response.accessToken, expiresAt)
        response.accessToken
    }
}

@Serializable
private data class EbayTokenResponse(
    val accessToken: String,
    val expiresIn: Long,
)
