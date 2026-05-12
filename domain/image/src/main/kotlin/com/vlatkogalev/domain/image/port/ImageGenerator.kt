package com.vlatkogalev.domain.image.port

interface ImageGenerator {
    suspend fun generate(prompt: String, options: GenerationOptions): GeneratedImage
}

data class GenerationOptions(
    val width: Int = 1024,
    val height: Int = 1024,
    val model: String? = null,
)

data class GeneratedImage(
    val bytes: ByteArray,
    val mimeType: String,
    val prompt: String,
)
