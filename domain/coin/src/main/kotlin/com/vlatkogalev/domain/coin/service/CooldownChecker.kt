package com.vlatkogalev.domain.coin.service

import com.vlatkogalev.domain.coin.model.ConfidenceConfig
import com.vlatkogalev.domain.coin.repository.EnrichmentAttemptsRepository
import java.time.Duration
import java.time.Instant

sealed interface CooldownResult {
    data object Expired : CooldownResult
    data class Active(val lastResult: String) : CooldownResult
}

class CooldownChecker(
    private val enrichmentAttemptsRepository: EnrichmentAttemptsRepository,
) {
    suspend fun check(fingerprintHash: String, now: Instant): CooldownResult {
        val attempt = enrichmentAttemptsRepository.findByHash(fingerprintHash)
            ?: return CooldownResult.Expired

        val active = attempt.pipelineVersion == ConfidenceConfig.PIPELINE_VERSION &&
            Duration.between(attempt.lastAttemptAt, now).toHours() < ConfidenceConfig.COOLDOWN_HOURS

        return if (active) CooldownResult.Active(attempt.lastResult) else CooldownResult.Expired
    }
}
