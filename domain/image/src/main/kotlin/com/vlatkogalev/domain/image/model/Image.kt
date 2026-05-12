package com.vlatkogalev.domain.image.model

import java.time.Instant
import java.util.UUID

data class Image(
    val id: UUID,
    val userId: UUID,
    val prompt: String,
    val objectKey: String,
    val createdAt: Instant,
)