package com.braintreepayments.api.internal

internal class Time {

    /**
     * Returns the current time in milliseconds
     */
    val currentTime: Long
        get() = System.currentTimeMillis()
}
