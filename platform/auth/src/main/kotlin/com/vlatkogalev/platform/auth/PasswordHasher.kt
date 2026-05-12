package com.vlatkogalev.platform.auth

import com.vlatkogalev.domain.user.service.UserPasswordHasher
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class PasswordHasher(
    private val iterations: Int = 65_536,
    private val keyLength: Int = 256,
) : UserPasswordHasher {
    private val random = SecureRandom()

    override fun hash(password: String): String {
        val salt = ByteArray(16)
        random.nextBytes(salt)
        val hash = pbkdf2(password, salt)
        return "$iterations:${Base64.getEncoder().encodeToString(salt)}:${Base64.getEncoder().encodeToString(hash)}"
    }

    override fun verify(password: String, encodedHash: String): Boolean {
        val parts = encodedHash.split(":")
        if (parts.size != 3) return false

        val storedIterations = parts[0].toIntOrNull() ?: return false
        val salt = runCatching { Base64.getDecoder().decode(parts[1]) }.getOrNull() ?: return false
        val expectedHash = runCatching { Base64.getDecoder().decode(parts[2]) }.getOrNull() ?: return false

        val actualHash = pbkdf2(password, salt, storedIterations)
        return actualHash.contentEquals(expectedHash)
    }

    private fun pbkdf2(password: String, salt: ByteArray, rounds: Int = iterations): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, rounds, keyLength)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }
}
