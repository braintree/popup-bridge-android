package com.braintreepayments.api.internal

import android.content.Context
import android.os.Build
import com.braintreepayments.api.popupbridge.BuildConfig

internal class DeviceRepository(
    private val context: Context,
) {

    fun getAppId(): String = context.packageName

    fun getAppName(): String {
        val applicationInfo = context.applicationInfo
        val packageManager = context.packageManager
        return packageManager.getApplicationLabel(applicationInfo).toString()
    }

    fun getAppVersion(): String {
        val packageManager = context.packageManager
        val packageName = context.packageName
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        return packageInfo.versionName ?: "VersionUnknown"
    }

    val deviceManufacturer: String
        get() = Build.MANUFACTURER

    val deviceModel: String
        get() = Build.MODEL

    val androidApiVersion: String
        get() = "Android API ${Build.VERSION.SDK_INT}"

    val sdkVersion: String
        get() = BuildConfig.VERSION_NAME

    val isDeviceEmulator: Boolean
        get() = Build.BRAND.startsWith("generic") &&
                Build.DEVICE.startsWith("generic") ||
                Build.FINGERPRINT.startsWith("generic") ||
                Build.FINGERPRINT.startsWith("unknown") ||
                Build.HARDWARE.contains("goldfish") ||
                Build.HARDWARE.contains("ranchu") ||
                Build.MODEL.contains("google_sdk") ||
                Build.MODEL.contains("Emulator") ||
                Build.MODEL.contains("Android SDK built for x86") ||
                Build.MANUFACTURER.contains("Genymotion") ||
                Build.PRODUCT.contains("sdk_google") ||
                Build.PRODUCT.contains("google_sdk") ||
                Build.PRODUCT.contains("sdk") ||
                Build.PRODUCT.contains("sdk_x86") ||
                Build.PRODUCT.contains("vbox86p") ||
                Build.PRODUCT.contains("emulator") ||
                Build.PRODUCT.contains("simulator")
}