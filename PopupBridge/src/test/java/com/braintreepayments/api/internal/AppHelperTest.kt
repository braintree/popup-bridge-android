package com.braintreepayments.api.internal

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppHelperTest {

    @Test
    fun `test isAppInstalled returns true when app is installed`() {
        val mockContext = mockk<Context>()
        val mockPackageManager = mockk<PackageManager>()
        every { mockContext.packageManager } returns mockPackageManager
        every { mockPackageManager.getApplicationInfo(any<String>(), any<Int>()) } returns mockk<ApplicationInfo>()

        val appHelper = AppHelper()
        val result = appHelper.isAppInstalled(mockContext, "com.example.app")

        assertTrue(result)
    }

    @Test
    fun `test isAppInstalled returns false when app is not installed`() {
        val mockContext = mockk<Context>()
        val mockPackageManager = mockk<PackageManager>()
        every { mockContext.packageManager } returns mockPackageManager
        every { mockPackageManager.getApplicationInfo(any<String>(), any<Int>()) } throws PackageManager.NameNotFoundException()

        val appHelper = AppHelper()
        val result = appHelper.isAppInstalled(mockContext, "com.example.app")

        assertFalse(result)
    }
}
