package com.vlatkogalev.app.jobs

import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * Schedules [EbayListingsJob.run] at a fixed rate.
 * Call [start] once during application startup, [stop] during shutdown.
 */
class MarketplaceJobScheduler(
    private val job: EbayListingsJob,
    private val initialDelaySeconds: Long = 15,
    private val intervalSeconds: Long = 600,
) {
    private val log = LoggerFactory.getLogger(MarketplaceJobScheduler::class.java)
    private val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "marketplace-listings-scheduler").also { it.isDaemon = true }
    }

    fun start() {
        log.info(
            "MarketplaceJobScheduler: scheduling eBay listings fetch every {}s (first run in {}s)",
            intervalSeconds, initialDelaySeconds,
        )
        executor.scheduleAtFixedRate(
            {
                try {
                    job.run()
                } catch (ex: Exception) {
                    log.error("MarketplaceJobScheduler: unhandled error in listings job", ex)
                }
            },
            initialDelaySeconds,
            intervalSeconds,
            TimeUnit.SECONDS,
        )
    }

    fun stop() {
        log.info("MarketplaceJobScheduler: shutting down")
        executor.shutdownNow()
    }
}
