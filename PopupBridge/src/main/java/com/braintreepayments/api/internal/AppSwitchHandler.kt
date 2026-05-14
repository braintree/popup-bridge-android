package com.braintreepayments.api.internal

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.core.net.toUri
import com.braintreepayments.api.PopupBridgeAnalytics
import com.braintreepayments.api.internal.PopupBridgeJavascriptInterface.Companion.POPUP_BRIDGE_URL_HOST
import java.lang.ref.WeakReference

internal class AppSwitchHandler(
    private val activityRef: WeakReference<ComponentActivity>,
    private val analyticsClient: AnalyticsClient,
    private val onOpenUrl: (String?) -> Unit,
    private val onError: (Exception) -> Unit,
    private val onCanceled: () -> Unit,
    private val onComplete: (Uri) -> Unit,
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var expectingAppSwitchReturn = false

    fun shouldHandleReturn(returnUri: Uri?): Boolean =
        returnUri != null && returnUri.isAppSwitchReturnUri()

    fun handleReturn(returnUri: Uri) {
        if (!expectingAppSwitchReturn) return
        expectingAppSwitchReturn = false

        analyticsClient.sendEvent(PopupBridgeAnalytics.POPUP_BRIDGE_APP_SWITCH_RETURNED)
        clearReturnIntentIfPresent()

        if (returnUri.isCancelUri()) {
            onCanceled()
        } else {
            onComplete(returnUri)
        }
    }

    fun launchApp(url: String?) {
        val activity = activityRef.get() ?: return
        mainHandler.post { launchAppOnMainThread(url, activity) }
    }

    fun clearReturnIntentIfPresent() {
        val activity = activityRef.get() ?: return
        val currentIntent = activity.intent ?: return
        val currentData = currentIntent.data ?: return

        if (currentData.host != POPUP_BRIDGE_URL_HOST) return

        activity.intent = Intent(currentIntent).apply { data = null }
    }

    private fun launchAppOnMainThread(url: String?, activity: ComponentActivity) {
        if (url.isNullOrBlank()) {
            onError(IllegalArgumentException("Invalid URL for app launch"))
            return
        }

        clearReturnIntentIfPresent()
        expectingAppSwitchReturn = true

        val uri = url.toUri()
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (uri.isPayPalAppSwitchUri()) {
                setPackage(PAYPAL_APP_PACKAGE)
            }
        }

        try {
            activity.startActivity(intent)
            analyticsClient.sendEvent(PopupBridgeAnalytics.POPUP_BRIDGE_APP_LAUNCHED)
        } catch (_: ActivityNotFoundException) {
            expectingAppSwitchReturn = false
            analyticsClient.sendEvent(PopupBridgeAnalytics.POPUP_BRIDGE_APP_LAUNCH_FAILED)
            onOpenUrl(url)
        }
    }

    private fun Uri.isPayPalAppSwitchUri(): Boolean {
        val normalizedHost = host?.removePrefix("www.")
        return scheme.equals("https", ignoreCase = true) &&
            normalizedHost == "paypal.com" &&
            path.orEmpty().startsWith("/app-switch-checkout")
    }

    private fun Uri.isAppSwitchReturnUri(): Boolean {
        if (host != POPUP_BRIDGE_URL_HOST) return false
        if (!fragment.isNullOrBlank()) return true
        return hasAppSwitchPath()
    }

    private fun Uri.isCancelUri(): Boolean {
        val normalizedPath = path.orEmpty().lowercase()
        return normalizedPath.contains("oncancel") || normalizedPath.contains("/cancel")
    }

    private fun Uri.hasAppSwitchPath(): Boolean {
        val normalizedPath = path.orEmpty().lowercase()
        return normalizedPath.contains("onapprove") ||
            normalizedPath.contains("onerror") ||
            normalizedPath.contains("/approve") ||
            normalizedPath.contains("/error") ||
            isCancelUri()
    }
}
