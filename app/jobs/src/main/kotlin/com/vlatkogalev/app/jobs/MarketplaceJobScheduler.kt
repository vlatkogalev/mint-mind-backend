package com.vlatkogalev.app.jobs

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class MarketplaceJobScheduler(
    private val listingsJob: EbayListingsJob,
    private val initialDelaySeconds: Long = 15,
    private val intervalSeconds: Long = 600,
) {
    private val handler = CoroutineExceptionHandler { _, e ->
        println("MarketplaceJobScheduler fatal error: ${e.message}")
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + handler)
    private var job: Job? = null

    fun start() {
        job = scope.launch {
            delay(initialDelaySeconds.seconds)
            while (isActive) {
                try {
                    listingsJob.run()
                } catch (e: Exception) {
                    println("MarketplaceJobScheduler error: ${e.message}")
                }
                delay(intervalSeconds.seconds)
            }
        }
    }

    fun stop() {
        job?.cancel()
    }
}
