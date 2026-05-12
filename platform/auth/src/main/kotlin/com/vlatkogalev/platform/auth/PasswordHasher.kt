package com.vlatkogalev.platform.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import com.vlatkogalev.domain.user.service.UserPasswordHasher

class PasswordHasher(
    private val cost: Int = 12,
) : UserPasswordHasher {
    override fun hash(password: String): String =
        BCrypt.withDefaults().hashToString(cost, password.toCharArray())

    override fun verify(password: String, encodedHash: String): Boolean =
        BCrypt.verifyer().verify(password.toCharArray(), encodedHash).verified
}
