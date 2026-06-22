package com.vlatkogalev.app.api.service

import com.vlatkogalev.domain.user.repository.UserRepository

class SessionMergeService(
    private val userRepository: UserRepository,
) {
    suspend fun mergeIfNeeded(installationId: String, authenticatedEmail: String) {
        val normalizedInstallationId = installationId.trim()
        if (normalizedInstallationId.isBlank()) return

        val authenticatedUserId = userRepository.findAuthIdentityByEmail(authenticatedEmail.trim().lowercase())?.userId
            ?: return

        val anonymousUser = userRepository.findUserByInstallationId(normalizedInstallationId) ?: return
        if (!anonymousUser.isAnonymous) return
        if (anonymousUser.id == authenticatedUserId) return

        userRepository.mergeAnonymousInto(anonymousUser.id, authenticatedUserId)
    }
}
