package com.braintreepayments.api

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.braintreepayments.api.PopupBridgeAnalytics.POPUP_BRIDGE_CANCELED
import com.braintreepayments.api.PopupBridgeAnalytics.POPUP_BRIDGE_FAILED
import com.braintreepayments.api.PopupBridgeAnalytics.POPUP_BRIDGE_SUCCEEDED
import com.braintreepayments.api.internal.AnalyticsClient
import com.braintreepayments.api.internal.AnalyticsParamRepository
import com.braintreepayments.api.internal.PendingRequestRepository
import com.braintreepayments.api.internal.PopupBridgeJavascriptInterface
import com.braintreepayments.api.internal.PopupBridgeJavascriptInterface.Companion.POPUP_BRIDGE_URL_HOST
import com.braintreepayments.api.internal.PAYPAL_APP_PACKAGE
import com.braintreepayments.api.internal.isPayPalInstalled
import java.lang.ref.WeakReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject

class PopupBridgeClient @SuppressLint("SetJavaScriptEnabled") internal constructor(
    activity: ComponentActivity,
    webView: WebView,
    private val returnUrlScheme: String,
    private val popupBridgeWebViewClient: PopupBridgeWebViewClient,
    private val browserSwitchClient: BrowserSwitchClient,
    private val enablePopupBridgeAppSwitch: Boolean = false,
    private val pendingRequestRepository: PendingRequestRepository = PendingRequestRepository(activity.applicationContext),
    private val coroutineScope: CoroutineScope = activity.lifecycleScope,
    private val analyticsClient: AnalyticsClient = AnalyticsClient(
        context = activity.applicationContext,
        coroutineScope = activity.lifecycleScope,
    ),
    analyticsParamRepository: AnalyticsParamRepository = AnalyticsParamRepository.instance,
    popupBridgeJavascriptInterface: PopupBridgeJavascriptInterface = PopupBridgeJavascriptInterface(
        returnUrlScheme = returnUrlScheme,
        context = activity.applicationContext,
        enablePopupBridgeAppSwitch = enablePopupBridgeAppSwitch,
    ),
) {
    private val activityRef = WeakReference(activity)
    private val webViewRef = WeakReference(webView)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var navigationListener: PopupBridgeNavigationListener? = null
    private var messageListener: PopupBridgeMessageListener? = null
    private var errorListener: PopupBridgeErrorListener? = null

    /**
     * Browser-switch path only: ensures [handleReturnToApp] completes the pending browser request at most once
     * when both `onResume` and `onNewIntent` invoke it (singleTop / singleTask / singleInstance).
     *
     * Not used for native app-switch returns; see [expectingAppSwitchReturn].
     */
    @Volatile
    private var isHandlingReturnToApp = false

    /**
     * Set to `true` when [launchApp] starts an app-switch checkout so we only accept the following
     * popupbridgev1 deep link as that flow's return (ignores stale links). Cleared when
     * [handleAppSwitchReturn] runs or when launch falls back to the browser.
     */
    @Volatile
    private var expectingAppSwitchReturn = false

    /**
     * Create a new instance of [PopupBridgeClient].
     *
     * This will enable JavaScript in your WebView.
     *
     * @param activity The [ComponentActivity] that contains the [WebView].
     * @param webView The [WebView] to enable for PopupBridge.
     * @param returnUrlScheme The return url scheme to use for deep linking back into the application.
     * @param popupBridgeWebViewClient The [PopupBridgeWebViewClient] to use for handling web view events.
     * @param enablePopupBridgeAppSwitch When true, allows the SDK to launch the native PayPal app
     *   for checkout instead of opening a browser. Defaults to false for backward compatibility.
     *   This is specific to the popup bridge flow and is separate from the JS SDK's
     *   appSwitchWhenAvailable which controls non-webview mobile browser app switch.
     * @throws IllegalArgumentException If the activity is not valid or the fragment cannot be added.
     */
    @JvmOverloads
    constructor(
        activity: ComponentActivity,
        webView: WebView,
        returnUrlScheme: String,
        popupBridgeWebViewClient: PopupBridgeWebViewClient = PopupBridgeWebViewClient(),
        enablePopupBridgeAppSwitch: Boolean = false,
    ) : this(
        activity = activity,
        webView = webView,
        returnUrlScheme = returnUrlScheme,
        popupBridgeWebViewClient = popupBridgeWebViewClient,
        browserSwitchClient = BrowserSwitchClient(),
        enablePopupBridgeAppSwitch = enablePopupBridgeAppSwitch,
    )

    init {
        val activity = activityRef.get()
        requireNotNull(activity) { "Activity is null" }

        val webView = webViewRef.get()
        requireNotNull(webView) { "WebView is null" }

        analyticsParamRepository.reset()

        webView.settings.javaScriptEnabled = true
        webView.addJavascriptInterface(popupBridgeJavascriptInterface, POPUP_BRIDGE_NAME)
        webView.webViewClient = popupBridgeWebViewClient

        if (enablePopupBridgeAppSwitch && activity.applicationContext.isPayPalInstalled()) {
            analyticsClient.sendEvent(PopupBridgeAnalytics.POPUP_BRIDGE_APP_DETECTED)
        }

        with(popupBridgeJavascriptInterface) {
            onOpen = { url -> openUrl(url) }
            onLaunchApp = { url -> this@PopupBridgeClient.launchApp(url) }
            onSendMessage = { messageName, data ->
                messageListener?.onMessageReceived(messageName, data)
            }
        }
    }

    fun setNavigationListener(listener: PopupBridgeNavigationListener?) {
        navigationListener = listener
    }

    fun setMessageListener(listener: PopupBridgeMessageListener?) {
        messageListener = listener
    }

    fun setErrorListener(listener: PopupBridgeErrorListener?) {
        errorListener = listener
    }

    fun handleReturnToApp(intent: Intent) {
        // Only treat PopupBridge deep links with app-switch-style path/fragment data as
        // native app returns. Secure-browser fallback can also return to popupbridgev1.
        val returnUri = intent.data
        if (returnUri != null && returnUri.isAppSwitchReturnUri()) {
            handleAppSwitchReturn(returnUri)
            return
        }

        if (isHandlingReturnToApp) {
            return
        }
        isHandlingReturnToApp = true

        coroutineScope.launch {
            val pendingRequest = pendingRequestRepository.getPendingRequest()
            if (pendingRequest == null) {
                isHandlingReturnToApp = false
                return@launch
            }

            pendingRequestRepository.clearPendingRequest()

            when (val browserSwitchFinalResult = browserSwitchClient.completeRequest(intent, pendingRequest)) {
                is BrowserSwitchFinalResult.Success -> {
                    val returnUrl = browserSwitchFinalResult.returnUrl
                    if (returnUrl.isCancelUri()) {
                        runCanceledJavaScript()
                    } else {
                        runNotifyCompleteJavaScript(returnUrl)
                    }
                }
                is BrowserSwitchFinalResult.Failure -> runErrorJavaScript(
                    browserSwitchFinalResult.error.message ?: "Browser switch failed"
                )
                is BrowserSwitchFinalResult.NoResult -> runCanceledJavaScript()
            }
            clearPopupBridgeReturnIntentIfPresent("browser_switch_result_consumed")
            isHandlingReturnToApp = false
        }
    }

    /**
     * Entry point when the web page calls window.popupBridge.launchApp(url).
     * Posts to the main thread so [startActivity] is never called from a background thread
     * (JavascriptInterface callbacks can be invoked off the main thread).
     */
    private fun launchApp(url: String?) {
        val activity = activityRef.get() as? ComponentActivity ?: run {
            return
        }
        mainHandler.post { launchAppAssumingMainThread(url, activity) }
    }

    private fun launchAppAssumingMainThread(url: String?, activity: ComponentActivity) {
        if (url.isNullOrBlank()) {
            errorListener?.onError(IllegalArgumentException("Invalid URL for app launch"))
            return
        }

        clearPopupBridgeReturnIntentIfPresent("launching_new_app_switch")
        expectingAppSwitchReturn = true

        val uri = url?.toUri() ?: run {
            errorListener?.onError(IllegalArgumentException("Invalid URL for app launch"))
            return
        }

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
            openUrl(url)
        }
    }

    private fun Uri.isPayPalAppSwitchUri(): Boolean {
        val normalizedHost = host?.removePrefix("www.")
        return scheme.equals("https", ignoreCase = true) &&
            normalizedHost == "paypal.com" &&
            path.orEmpty().startsWith("/app-switch-checkout")
    }

    private fun Uri.isAppSwitchReturnUri(): Boolean {
        if (host != POPUP_BRIDGE_URL_HOST) {
            return false
        }

        if (!fragment.isNullOrBlank()) {
            return true
        }

        return hasAppSwitchPath()
    }

    private fun Uri.isCancelUri(): Boolean {
        val normalizedPath = path.orEmpty().lowercase()
        val normalizedFragment = fragment.orEmpty().lowercase()
        return normalizedPath.contains("oncancel") ||
            normalizedPath.contains("/cancel") ||
            normalizedFragment.contains("oncancel") ||
            normalizedFragment.contains("/cancel")
    }

    private fun Uri.hasAppSwitchPath(): Boolean {
        val normalizedPath = path.orEmpty().lowercase()
        return normalizedPath.contains("onapprove") ||
            normalizedPath.contains("onerror") ||
            normalizedPath.contains("/approve") ||
            normalizedPath.contains("/error") ||
            isCancelUri()
    }

    /**
     * Handles the return deep link from a native PayPal/Venmo app switch.
     *
     * Runs only when [expectingAppSwitchReturn] is true (set by [launchApp] before starting the
     * native app) so stale popupbridgev1 links do not complete or cancel the wrong session.
     */
    private fun handleAppSwitchReturn(returnUri: Uri) {
        if (!expectingAppSwitchReturn) {
            return
        }
        expectingAppSwitchReturn = false

        analyticsClient.sendEvent(PopupBridgeAnalytics.POPUP_BRIDGE_APP_SWITCH_RETURNED)
        clearPopupBridgeReturnIntentIfPresent("app_switch_return_consumed")

        if (returnUri.isCancelUri()) {
            runCanceledJavaScript()
        } else {
            runNotifyCompleteJavaScript(returnUri)
        }
    }

    private fun openUrl(url: String?) {
        analyticsClient.sendEvent(PopupBridgeAnalytics.POPUP_BRIDGE_STARTED)

        val activity = activityRef.get() ?: run {
            return
        }
        val browserSwitchOptions = BrowserSwitchOptions()
            .requestCode(REQUEST_CODE)
            .url(url?.toUri())
            .returnUrlScheme(returnUrlScheme)
        val browserSwitchStartResult = browserSwitchClient.start(activity, browserSwitchOptions)
        when (browserSwitchStartResult) {
            is BrowserSwitchStartResult.Started -> {
                coroutineScope.launch {
                    pendingRequestRepository.storePendingRequest(browserSwitchStartResult.pendingRequest)
                }
            }

            is BrowserSwitchStartResult.Failure -> {
                errorListener?.onError(browserSwitchStartResult.error)
            }
        }
        navigationListener?.onUrlOpened(url)
    }

    private fun runNotifyCompleteJavaScript(returnUri: Uri) {
        if (returnUri.host != POPUP_BRIDGE_URL_HOST) {
            return
        }

        val payLoadJson = JSONObject()
        val queryItems = JSONObject()

        for (queryParam in returnUri.queryParameterNames) {
            try {
                queryItems.put(queryParam, returnUri.getQueryParameter(queryParam))
            } catch (e: JSONException) {
                val error = "new Error('Failed to parse query items from return URL. " +
                        e.localizedMessage + "')"
                runErrorJavaScript(error)
                return
            }
        }

        try {
            payLoadJson.put("path", returnUri.path)
            payLoadJson.put("queryItems", queryItems)
            payLoadJson.put("hash", returnUri.fragment)
        } catch (ignored: JSONException) {
        }

        analyticsClient.sendEvent(POPUP_BRIDGE_SUCCEEDED)

        val successJavascript = String.format(ON_COMPLETE_JAVA_SCRIPT, null, payLoadJson.toString())
        runJavaScriptInWebView(successJavascript)
    }

    private fun runErrorJavaScript(error: String) {
        analyticsClient.sendEvent(POPUP_BRIDGE_FAILED)

        val successJavascript = String.format(ON_COMPLETE_JAVA_SCRIPT, error, null)
        runJavaScriptInWebView(successJavascript)
    }

    private fun runCanceledJavaScript() {
        analyticsClient.sendEvent(POPUP_BRIDGE_CANCELED)

        runJavaScriptInWebView(
            ""
                    + "function notifyCanceled() {"
                    + "  if (typeof window.popupBridge.onCancel === 'function') {"
                    + "    window.popupBridge.onCancel();"
                    + "  } else {"
                    + "    window.popupBridge.onComplete(null, null);"
                    + "  }"
                    + "}"
                    + ""
                    + "if (document.readyState === 'complete') {"
                    + "  notifyCanceled();"
                    + "} else {"
                    + "  window.addEventListener('load', function () {"
                    + "    notifyCanceled();"
                    + "  });"
                    + "}"
        )
    }

    private fun clearPopupBridgeReturnIntentIfPresent(reason: String) {
        val activity = activityRef.get() ?: return
        val currentIntent = activity.intent ?: return
        val currentData = currentIntent.data ?: return

        if (currentData.host != POPUP_BRIDGE_URL_HOST) {
            return
        }

        activity.intent = Intent(currentIntent).apply {
            data = null
        }
    }

    private fun runJavaScriptInWebView(script: String) {
        webViewRef.get()?.post(
            Runnable { webViewRef.get()?.evaluateJavascript(script, null) }
        )
    }

    companion object {
        private const val REQUEST_CODE: Int = 1
        private const val POPUP_BRIDGE_NAME: String = "popupBridge"

        private const val ON_COMPLETE_JAVA_SCRIPT = (""
                + "function notifyComplete() {"
                + "  window.popupBridge.onComplete(%s, %s);"
                + "}"
                + ""
                + "if (document.readyState === 'complete') {"
                + "  notifyComplete();"
                + "} else {"
                + "  window.addEventListener('load', function () {"
                + "    notifyComplete();"
                + "  });"
                + "}")
    }
}
