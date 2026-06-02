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

class MarketplaceJobScheduler(
    private val job: EbayListingsJob,
    private val initialDelaySeconds: Long = 15,
    private val intervalSeconds: Long = 600,
) {
    private val log = LoggerFactory.getLogger(MarketplaceJobScheduler::class.java)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var scheduledJob: Job? = null

    fun start() {
        log.info(
            "MarketplaceJobScheduler: scheduling eBay listings fetch every {}s (first run in {}s)",
            intervalSeconds, initialDelaySeconds,
        )
        scheduledJob = scope.launch {
            delay(initialDelaySeconds * 1000L)
            while (isActive) {
                try {
                    job.run()
                } catch (ex: Exception) {
                    log.error("MarketplaceJobScheduler: unhandled error in listings job", ex)
                }
                delay(intervalSeconds * 1000L)
            }
        }
    }

    fun stop() {
        log.info("MarketplaceJobScheduler: shutting down")
        scheduledJob?.cancel()
        scope.cancel()
    }
}
