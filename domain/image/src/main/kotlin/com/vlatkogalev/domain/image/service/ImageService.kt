package com.vlatkogalev.domain.image.service

import com.vlatkogalev.domain.image.model.Image
import com.vlatkogalev.platform.core.Result

interface ImageService {
    suspend fun generate(userId: Long, prompt: String): Result<Image>
}
