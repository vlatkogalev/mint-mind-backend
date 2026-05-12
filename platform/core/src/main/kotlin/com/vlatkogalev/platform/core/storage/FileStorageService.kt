package com.vlatkogalev.platform.core.storage

import java.net.URL

interface FileStorageService {
    fun createPresignedUpload(key: String, ttlSeconds: Long = 900): URL
    fun createPresignedDownload(key: String, ttlSeconds: Long = 900): URL
    fun createUploadSession(prefix: String): UploadSession
}

data class UploadSession(
    val sessionId: String,
    val objectPrefix: String,
)
