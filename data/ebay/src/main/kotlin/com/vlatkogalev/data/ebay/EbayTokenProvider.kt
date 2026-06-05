package com.vlatkogalev.data.ebay

import com.vlatkogalev.platform.core.config.EbayConfig
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
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
    @SerialName("access_token")
    val accessToken: String,
    @SerialName("expires_in")
    val expiresIn: Long,
)
