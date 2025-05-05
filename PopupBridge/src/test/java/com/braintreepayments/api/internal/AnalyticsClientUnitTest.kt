package com.braintreepayments.api.internal

import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AnalyticsClientUnitTest {

    private val analyticsApi: AnalyticsApi = mockk(relaxed = true)

    @Test
    fun `sendEvent should call analyticsApi execute with correct event name`() = runTest {
        val coroutineScope: CoroutineScope = TestScope(StandardTestDispatcher(testScheduler))
        val subject = AnalyticsClient(mockk(), coroutineScope, analyticsApi)
        val eventName = "test-event"

        subject.sendEvent(eventName)
        testScheduler.advanceUntilIdle()

        coVerify { analyticsApi.execute(eventName) }
    }
}
