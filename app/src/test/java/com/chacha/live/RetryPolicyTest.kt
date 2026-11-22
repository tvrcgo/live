package com.chacha.live

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RetryPolicyTest {

    @Test
    fun delayBacksOffThenStops() {
        assertEquals(1_000L, RetryPolicy.nextDelay(0))
        assertEquals(3_000L, RetryPolicy.nextDelay(1))
        assertEquals(6_000L, RetryPolicy.nextDelay(2))
        assertNull(RetryPolicy.nextDelay(3))
    }

    @Test
    fun invalidAttemptHasNoDelay() {
        assertNull(RetryPolicy.nextDelay(-1))
        assertNull(RetryPolicy.nextDelay(Int.MAX_VALUE))
    }
}
