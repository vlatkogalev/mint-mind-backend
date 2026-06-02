package com.vlatkogalev.domain.user.service

interface UserPasswordHasher {
    suspend fun hash(password: String): String
    suspend fun verify(password: String, encodedHash: String): Boolean
}