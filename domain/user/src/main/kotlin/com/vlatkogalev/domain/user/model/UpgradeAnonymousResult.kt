package com.vlatkogalev.domain.user.model

sealed interface UpgradeAnonymousResult {
    data class Success(val user: UserAccount) : UpgradeAnonymousResult
    data object NotFound : UpgradeAnonymousResult
    data object NotAnonymous : UpgradeAnonymousResult
}