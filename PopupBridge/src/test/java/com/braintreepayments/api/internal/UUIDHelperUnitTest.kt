package com.braintreepayments.api.internal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UUIDHelperUnitTest {

    @Test
    fun `formattedUUID should return a UUID without dashes`() {
        val uuidHelper = UUIDHelper()

        val formattedUUID = uuidHelper.formattedUUID

        assertEquals(32, formattedUUID.length)
        assertTrue(formattedUUID.matches(Regex("^[a-fA-F0-9]+$")))
    }

    @Test
    fun `formattedUUID should generate unique values`() {
        val uuidHelper = UUIDHelper()

        val uuid1 = uuidHelper.formattedUUID
        val uuid2 = uuidHelper.formattedUUID

        assertTrue(uuid1 != uuid2)
    }
}
