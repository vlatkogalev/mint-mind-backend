package com.vlatkogalev.platform.logging

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.request.path
import org.slf4j.LoggerFactory
import org.slf4j.event.Level

fun Application.configureLogging() {
    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }
    }
}

class StructuredLogger(private val name: String) {
    private val logger = LoggerFactory.getLogger(name)

    fun info(message: String, metadata: Map<String, Any?> = emptyMap()) {
        logger.info("{} {}", message, metadata)
    }

    fun error(message: String, metadata: Map<String, Any?> = emptyMap(), throwable: Throwable? = null) {
        logger.error("{} {}", message, metadata, throwable)
    }
}
