package com.braintreepayments.api.internal

import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class AppHelper {

    companion object {
        private const val NO_FLAGS = 0
    }

    fun isAppInstalled(context: Context, packageName: String): Boolean {
        val packageManager: PackageManager = context.packageManager
        try {
            packageManager.getApplicationInfo(packageName, NO_FLAGS)
            return true;
        } catch (_: PackageManager.NameNotFoundException) {
            return false;
        }
    }
}
