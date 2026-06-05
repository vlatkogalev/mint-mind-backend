package com.vlatkogalev.domain.image.model

data class GenerationOptions(
    val width: Int = 1024,
    val height: Int = 1024,
    val model: String? = null,
)