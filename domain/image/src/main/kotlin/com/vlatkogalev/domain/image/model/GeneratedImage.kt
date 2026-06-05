package com.vlatkogalev.domain.image.model

@Suppress("ArrayInDataClass")
data class GeneratedImage(
    val bytes: ByteArray,
    val mimeType: String,
    val prompt: String,
)