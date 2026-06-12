package com.vlatkogalev.platform.core.storage

import java.net.URL

interface FileStorageService {
    suspend fun createPresignedUpload(key: String, ttlSeconds: Long = 900): URL
    suspend fun createPresignedDownload(key: String, ttlSeconds: Long = 900): URL
    fun createUploadSession(prefix: String): UploadSession
    fun publicUrl(key: String): String
}

data class UploadSession(
    val sessionId: String,
    val objectPrefix: String,
)
