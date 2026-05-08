package com.vlatkogalev.platform.core.time

fun interface TimeProvider {
    fun nowMillis(): Long

    data object System : TimeProvider {
        override fun nowMillis(): Long = java.lang.System.currentTimeMillis()
    }
}
