package com.vlatkogalev.app.jobs

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

class NewsJobScheduler(
    private val fetcher: RssFeedFetcher,
    private val initialDelaySeconds: Long = 10,
    private val intervalSeconds: Long = 43200,
) {
    private val log = LoggerFactory.getLogger(NewsJobScheduler::class.java)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var job: Job? = null

    fun start() {
        log.info(
            "NewsJobScheduler: scheduling RSS fetch every {}s (first run in {}s)",
            intervalSeconds, initialDelaySeconds,
        )
        job = scope.launch {
            delay(initialDelaySeconds * 1000L)
            while (isActive) {
                try {
                    fetcher.run()
                } catch (ex: Exception) {
                    log.error("NewsJobScheduler: unhandled error in feed fetcher", ex)
                }
                delay(intervalSeconds * 1000L)
            }
        }
    }

    fun stop() {
        log.info("NewsJobScheduler: shutting down")
        job?.cancel()
        scope.cancel()
    }
}
