package com.vlatkogalev.domain.image.model

import java.time.Instant

data class Image(
    val id: Long,
    val userId: Long,
    val prompt: String,
    val objectKey: String,
    val createdAt: Instant,
)
