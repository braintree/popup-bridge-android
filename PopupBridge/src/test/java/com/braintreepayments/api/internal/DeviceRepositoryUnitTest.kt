package com.braintreepayments.api.internal

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [DeviceRepository].
 *
 * Note that the functions/values that rely on `Build` or `BuildConfig` cannot be mocked and are therefore are not
 * unit tested.
 */
class DeviceRepositoryUnitTest {

    private val mockContext = mockk<Context>(relaxed = true)
    private val mockPackageManager = mockk<PackageManager>(relaxed = true)
    private val mockApplicationInfo = mockk<ApplicationInfo>(relaxed = true)
    private val mockPackageInfo = mockk<PackageInfo>(relaxed = true)

    private val deviceRepository = DeviceRepository(mockContext)

    @Before
    fun setUp() {
        every { mockContext.applicationInfo } returns mockApplicationInfo
        every { mockContext.packageManager } returns mockPackageManager
    }

    @Test
    fun `getAppId returns package name`() {
        every { mockContext.packageName } returns "com.example.app"
        assertEquals("com.example.app", deviceRepository.getAppId())
    }

    @Test
    fun `getAppName returns application label`() {
        every { mockPackageManager.getApplicationLabel(mockApplicationInfo) } returns "Example App"

        assertEquals("Example App", deviceRepository.getAppName())
    }

    @Test
    fun `getAppVersion returns version name`() {
        every { mockContext.packageName } returns "com.example.app"
        every { mockPackageManager.getPackageInfo("com.example.app", 0) } returns mockPackageInfo
        mockPackageInfo.versionName = "1.0.0"

        assertEquals("1.0.0", deviceRepository.getAppVersion())
    }

    @Test
    fun `getAppVersion returns VersionUnknown when version name is null`() {
        every { mockContext.packageName } returns "com.example.app"
        every { mockPackageManager.getPackageInfo("com.example.app", 0) } returns mockPackageInfo
        mockPackageInfo.versionName = null

        assertEquals("VersionUnknown", deviceRepository.getAppVersion())
    }
}
