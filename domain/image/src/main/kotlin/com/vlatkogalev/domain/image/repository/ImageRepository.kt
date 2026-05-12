package com.vlatkogalev.domain.image.repository

import com.vlatkogalev.domain.image.model.Image

interface ImageRepository {
    suspend fun save(image: Image): Image
    suspend fun findByUserId(userId: Long): List<Image>
}
