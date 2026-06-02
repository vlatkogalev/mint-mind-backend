package com.vlatkogalev.app.api.service

import com.vlatkogalev.domain.coin.repository.CoinRepository
import com.vlatkogalev.domain.user.repository.UserRepository

class SessionMergeService(
    private val userRepository: UserRepository,
    private val coinRepository: CoinRepository,
) {
    suspend fun mergeIfNeeded(installationId: String, authenticatedEmail: String) {
        val normalizedInstallationId = installationId.trim()
        if (normalizedInstallationId.isBlank()) return

        val authenticatedUserId = userRepository.findAuthIdentityByEmail(authenticatedEmail.trim().lowercase())?.userId
            ?: return

        val anonymousUser = userRepository.findUserByInstallationId(normalizedInstallationId) ?: return
        if (!anonymousUser.isAnonymous) return
        if (anonymousUser.id == authenticatedUserId) return

        coinRepository.reassignFromUser(anonymousUser.id, authenticatedUserId)
        userRepository.deleteById(anonymousUser.id)
    }
}
