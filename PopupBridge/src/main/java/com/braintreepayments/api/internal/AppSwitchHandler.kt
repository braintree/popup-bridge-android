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
        expectingAppSwitchReturn && returnUri != null && returnUri.isAppSwitchReturnUri()

    fun handleNoResult() {
        if (!expectingAppSwitchReturn) return
        expectingAppSwitchReturn = false
        onCanceled()
    }

    fun handleReturn(returnUri: Uri) {
        if (!expectingAppSwitchReturn) return
        expectingAppSwitchReturn = false

        clearReturnIntentIfPresent()

        if (returnUri.isCancelUri()) {
            analyticsClient.sendEvent(PopupBridgeAnalytics.POPUP_BRIDGE_APP_SWITCH_CANCELED)
            onCanceled()
        } else {
            analyticsClient.sendEvent(PopupBridgeAnalytics.POPUP_BRIDGE_APP_SWITCH_SUCCEEDED)
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

        val uri = url.toUri()
        if (!uri.isPayPalAppSwitchUri() && !uri.isVenmoAppSwitchUri()) {
            onError(IllegalArgumentException("URL is not a valid PayPal or Venmo app-switch URI: $url"))
            return
        }

        clearReturnIntentIfPresent()
        expectingAppSwitchReturn = true
        val targetUri = if (uri.isVenmoAppSwitchUri()) uri.rewriteToVenmoHost() else uri
        val intent = Intent(Intent.ACTION_VIEW, targetUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (targetUri.isPayPalAppSwitchUri()) {
                setPackage(PAYPAL_APP_PACKAGE)
            } else if (uri.isVenmoAppSwitchUri()) {
                setPackage(VENMO_APP_PACKAGE)
            }
        }

        try {
            activity.startActivity(intent)
            analyticsClient.sendEvent(PopupBridgeAnalytics.POPUP_BRIDGE_APP_SWITCH_STARTED)
        } catch (_: ActivityNotFoundException) {
            expectingAppSwitchReturn = false
            analyticsClient.sendEvent(PopupBridgeAnalytics.POPUP_BRIDGE_APP_SWITCH_FAILED)
            onOpenUrl(url)
        }
    }

    private fun Uri.isPayPalAppSwitchUri(): Boolean {
        val normalizedHost = host?.removePrefix("www.")
        val isPayPal = normalizedHost == "paypal.com"
        val isSandbox = normalizedHost == "sandbox.paypal.com"
        return scheme.equals("https", ignoreCase = true) &&
            (isPayPal || isSandbox) &&
            path.orEmpty().startsWith("/app-switch-checkout")
    }

    private fun Uri.isAppSwitchReturnUri(): Boolean {
        if (host != POPUP_BRIDGE_URL_HOST) return false
        if (!fragment.isNullOrBlank()) return true
        return hasAppSwitchPath()
    }

    private fun Uri.isCancelUri(): Boolean {
        val segments = path.orEmpty().lowercase().split("/").filter { it.isNotEmpty() }
        return segments.any { it == "oncancel" || it == "cancel" }
    }

    private fun Uri.hasAppSwitchPath(): Boolean {
        val segments = path.orEmpty().lowercase().split("/").filter { it.isNotEmpty() }
        return segments.any { it == "onapprove" || it == "approve" || it == "onerror" || it == "error" } ||
            isCancelUri()
    }
}

internal fun Uri.isVenmoAppSwitchUri(): Boolean {
    return scheme.equals("https", ignoreCase = true) &&
        host.equals("account.venmo.com", ignoreCase = true) &&
        path.orEmpty().startsWith("/braintree/checkout")
}

// account.venmo.com/braintree/checkout has no intent filter in the Venmo app.
// venmo.com has a broad catch-all filter that handles any path including /braintree/checkout.
internal fun Uri.rewriteToVenmoHost(): Uri = buildUpon().authority("venmo.com").build()
