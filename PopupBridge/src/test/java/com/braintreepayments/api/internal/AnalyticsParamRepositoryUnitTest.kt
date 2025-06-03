package com.braintreepayments.api.internal

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class AnalyticsParamRepositoryUnitTest {

    private lateinit var uuidHelper: UUIDHelper
    private lateinit var analyticsParamRepository: AnalyticsParamRepository

    @Before
    fun setup() {
        uuidHelper = mockk()
        analyticsParamRepository = AnalyticsParamRepository(uuidHelper)
    }

    @Test
    fun `sessionId generates a new UUID if not initialized`() {
        val mockUUID = "mock-uuid-1"
        every { uuidHelper.formattedUUID } returns mockUUID

        val sessionId = analyticsParamRepository.sessionId

        assertEquals(mockUUID, sessionId)
        verify { uuidHelper.formattedUUID }
    }

    @Test
    fun `sessionId returns the same value once initialized`() {
        val mockUUID = "mock-uuid-1"
        every { uuidHelper.formattedUUID } returns mockUUID

        val sessionId1 = analyticsParamRepository.sessionId
        val sessionId2 = analyticsParamRepository.sessionId

        assertEquals(mockUUID, sessionId1)
        assertEquals(mockUUID, sessionId2)
        verify(exactly = 1) { uuidHelper.formattedUUID }
    }

    @Test
    fun `reset generates a new sessionId`() {
        val mockUUID1 = "mock-uuid-1"
        val mockUUID2 = "mock-uuid-2"
        every { uuidHelper.formattedUUID } returnsMany listOf(mockUUID1, mockUUID2)

        val sessionId1 = analyticsParamRepository.sessionId
        analyticsParamRepository.reset()
        val sessionId2 = analyticsParamRepository.sessionId

        assertNotEquals(sessionId1, sessionId2)
        assertEquals(mockUUID2, sessionId2)
        verify(exactly = 2) { uuidHelper.formattedUUID }
    }
}
