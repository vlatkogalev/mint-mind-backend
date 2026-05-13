package com.vlatkogalev.domain.image.repository

import com.vlatkogalev.domain.image.model.Image
import java.util.UUID

interface ImageRepository {
    suspend fun save(image: Image): Image
    suspend fun findByUserId(userId: UUID): List<Image>
}
