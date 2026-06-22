package com.vlatkogalev.app.jobs

import com.vlatkogalev.platform.core.StructuredLogger
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class NewsJobScheduler(
    private val rssFeedFetcher: RssFeedFetcher,
    private val initialDelaySeconds: Long = 10,
    private val intervalSeconds: Long = 43200,
) {
    private val logger = StructuredLogger("NewsJobScheduler")
    private val handler = CoroutineExceptionHandler { _, e ->
        logger.error("Fatal error in scheduler", throwable = e)
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + handler)
    private var job: Job? = null

    fun start() {
        job = scope.launch {
            delay(initialDelaySeconds.seconds)
            while (isActive) {
                try {
                    rssFeedFetcher.run()
                } catch (e: Exception) {
                    logger.error("Job execution failed", throwable = e)
                }
                delay(intervalSeconds.seconds)
            }
        }
    }

    fun stop() {
        job?.cancel()
    }
}
