package com.braintreepayments.api

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
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
    private val pendingRequestRepository: PendingRequestRepository = PendingRequestRepository(activity.applicationContext),
    private val coroutineScope: CoroutineScope = activity.lifecycleScope,
    private val analyticsClient: AnalyticsClient = AnalyticsClient(
        context = activity.applicationContext,
        coroutineScope = activity.lifecycleScope,
    ),
    analyticsParamRepository: AnalyticsParamRepository = AnalyticsParamRepository.instance,
    popupBridgeJavascriptInterface: PopupBridgeJavascriptInterface = PopupBridgeJavascriptInterface(returnUrlScheme),
) {
    private val activityRef = WeakReference(activity)
    private val webViewRef = WeakReference(webView)

    private var navigationListener: PopupBridgeNavigationListener? = null
    private var messageListener: PopupBridgeMessageListener? = null
    private var errorListener: PopupBridgeErrorListener? = null

    /**
     * Ensures that [handleReturnToApp] is only called once, even if both `onResume` and `onNewIntent` invoke it.
     *
     * If the host app's Activity has a launch type of singleTop, singleTask, or singleInstance, [handleReturnToApp]
     * should be called in both onResume and onNewIntent. This is to cover all cases where the user cancels the flow.
     *
     * Since the [PendingRequestRepository] functions are suspend functions, we need to ensure that the
     * [handleReturnToApp] logic is only invoked once.
     */
    @Volatile
    private var isHandlingReturnToApp = false

    /**
     * Create a new instance of [PopupBridgeClient].
     *
     * This will enable JavaScript in your WebView.
     *
     * @param activity The [ComponentActivity] that contains the [WebView].
     * @param webView The [WebView] to enable for PopupBridge.
     * @param returnUrlScheme The return url scheme to use for deep linking back into the application.
     * @param popupBridgeWebViewClient The [PopupBridgeWebViewClient] to use for handling web view events.
     * @throws IllegalArgumentException If the activity is not valid or the fragment cannot be added.
     */
    @JvmOverloads
    constructor(
        activity: ComponentActivity,
        webView: WebView,
        returnUrlScheme: String,
        popupBridgeWebViewClient: PopupBridgeWebViewClient = PopupBridgeWebViewClient()
    ) : this(
        activity = activity,
        webView = webView,
        returnUrlScheme = returnUrlScheme,
        popupBridgeWebViewClient = popupBridgeWebViewClient,
        browserSwitchClient = BrowserSwitchClient()
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

        with(popupBridgeJavascriptInterface) {
            onOpen = { url -> openUrl(url) }
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
        if (isHandlingReturnToApp) return
        isHandlingReturnToApp = true

        coroutineScope.launch {
            val pendingRequest = pendingRequestRepository.getPendingRequest()
            if (pendingRequest == null) {
                isHandlingReturnToApp = false
                return@launch
            }

            pendingRequestRepository.clearPendingRequest()

            when (val browserSwitchFinalResult = browserSwitchClient.completeRequest(intent, pendingRequest)) {
                is BrowserSwitchFinalResult.Success -> runNotifyCompleteJavaScript(browserSwitchFinalResult.returnUrl)
                is BrowserSwitchFinalResult.Failure -> runErrorJavaScript(
                    browserSwitchFinalResult.error.message ?: "Browser switch failed"
                )
                is BrowserSwitchFinalResult.NoResult -> runCanceledJavaScript()
            }
            isHandlingReturnToApp = false
        }
    }

    private fun openUrl(url: String?) {
        analyticsClient.sendEvent(PopupBridgeAnalytics.POPUP_BRIDGE_STARTED)

        val activity = activityRef.get() ?: return
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
        if (returnUri.host != POPUP_BRIDGE_URL_HOST) return

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
