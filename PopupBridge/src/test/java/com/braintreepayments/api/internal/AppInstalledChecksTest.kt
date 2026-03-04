package com.braintreepayments.api.internal

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [isPayPalInstalled] and [isVenmoInstalled] extensions (Popup Bridge++ app detection).
 */
class AppInstalledChecksTest {

    @Test
    fun `isPayPalInstalled returns true when PayPal app is installed`() {
        val mockContext = mockk<Context>()
        val mockPackageManager = mockk<PackageManager>()
        every { mockContext.packageManager } returns mockPackageManager
        every {
            mockPackageManager.getApplicationInfo("com.paypal.android.p2pmobile", 0)
        } returns mockk<ApplicationInfo>()

        assertTrue(mockContext.isPayPalInstalled())
    }

    @Test
    fun `isPayPalInstalled returns false when PayPal app is not installed`() {
        val mockContext = mockk<Context>()
        val mockPackageManager = mockk<PackageManager>()
        every { mockContext.packageManager } returns mockPackageManager
        every {
            mockPackageManager.getApplicationInfo("com.paypal.android.p2pmobile", 0)
        } throws PackageManager.NameNotFoundException()

        assertFalse(mockContext.isPayPalInstalled())
    }

    @Test
    fun `isVenmoInstalled returns true when Venmo app is installed`() {
        val mockContext = mockk<Context>()
        val mockPackageManager = mockk<PackageManager>()
        every { mockContext.packageManager } returns mockPackageManager
        every { mockPackageManager.getApplicationInfo("com.venmo", 0) } returns mockk<ApplicationInfo>()

        assertTrue(mockContext.isVenmoInstalled())
    }

    @Test
    fun `isVenmoInstalled returns false when Venmo app is not installed`() {
        val mockContext = mockk<Context>()
        val mockPackageManager = mockk<PackageManager>()
        every { mockContext.packageManager } returns mockPackageManager
        every { mockPackageManager.getApplicationInfo("com.venmo", 0) } throws PackageManager.NameNotFoundException()

        assertFalse(mockContext.isVenmoInstalled())
    }
}
