package com.vlatkogalev.app.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateUploadUrlsRequest(
    val fileCount: Int,
)

@Serializable
data class PresignedUploadUrl(
    val objectKey: String,
    val uploadUrl: String,
)

@Serializable
data class CreateUploadUrlsResponse(
    val sessionId: String,
    val uploads: List<PresignedUploadUrl>,
)

@Serializable
data class CreateDownloadUrlsRequest(
    val objectKeys: List<String>,
)

@Serializable
data class PresignedDownloadUrl(
    val objectKey: String,
    val downloadUrl: String,
)

@Serializable
data class CreateDownloadUrlsResponse(
    val downloads: List<PresignedDownloadUrl>,
)
