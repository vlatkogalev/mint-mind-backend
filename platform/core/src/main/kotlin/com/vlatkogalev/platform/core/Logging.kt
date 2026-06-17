package com.vlatkogalev.platform.core

import org.slf4j.LoggerFactory

class StructuredLogger(private val name: String) {
    private val logger = LoggerFactory.getLogger(name)

    fun info(message: String, metadata: Map<String, Any?> = emptyMap()) {
        logger.info("{} {}", message, metadata)
    }

    fun error(message: String, metadata: Map<String, Any?> = emptyMap(), throwable: Throwable? = null) {
        logger.error("{} {}", message, metadata, throwable)
    }
}
