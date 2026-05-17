package com.vlatkogalev.app.jobs

import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * Schedules [RssFeedFetcher.run] at a fixed rate.
 * Call [start] once during application startup, [stop] during shutdown.
 */
class NewsJobScheduler(
    private val fetcher: RssFeedFetcher,
    private val initialDelaySeconds: Long = 10,
    private val intervalSeconds: Long = 43200,           // every 12 hours
) {
    private val log = LoggerFactory.getLogger(NewsJobScheduler::class.java)
    private val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "news-feed-scheduler").also { it.isDaemon = true }
    }

    fun start() {
        log.info(
            "NewsJobScheduler: scheduling RSS fetch every {}s (first run in {}s)",
            intervalSeconds, initialDelaySeconds,
        )
        executor.scheduleAtFixedRate(
            {
                try { fetcher.run() } catch (ex: Exception) {
                    log.error("NewsJobScheduler: unhandled error in feed fetcher", ex)
                }
            },
            initialDelaySeconds,
            intervalSeconds,
            TimeUnit.SECONDS,
        )
    }

    fun stop() {
        log.info("NewsJobScheduler: shutting down")
        executor.shutdownNow()
    }
}
