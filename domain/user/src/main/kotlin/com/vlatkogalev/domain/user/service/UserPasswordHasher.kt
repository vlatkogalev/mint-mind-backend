package com.vlatkogalev.domain.user.service

interface UserPasswordHasher {
    fun hash(password: String): String
    fun verify(password: String, encodedHash: String): Boolean
}