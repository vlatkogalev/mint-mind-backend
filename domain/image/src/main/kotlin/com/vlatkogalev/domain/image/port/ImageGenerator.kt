package com.vlatkogalev.domain.image.port

import com.vlatkogalev.domain.image.model.GeneratedImage
import com.vlatkogalev.domain.image.model.GenerationOptions

interface ImageGenerator {
    suspend fun generate(prompt: String, options: GenerationOptions): GeneratedImage
}
