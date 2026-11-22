package com.chacha.live

object RetryPolicy {

    private val delays = longArrayOf(1_000, 3_000, 6_000)

    fun nextDelay(attempt: Int): Long? {
        return delays.getOrNull(attempt)
    }
}
