package com.braintreepayments.api

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import org.json.JSONException
import org.json.JSONObject
import java.lang.ref.WeakReference

class PopupBridgeClient @SuppressLint("SetJavaScriptEnabled") internal constructor(
    private val activityRef: WeakReference<FragmentActivity>,
    private val webViewRef: WeakReference<WebView>,
    private val returnUrlScheme: String,
    private val browserSwitchClient: BrowserSwitchClient = BrowserSwitchClient(),
    private val pendingRequestRepository: PendingRequestRepository = PendingRequestRepository()
) {
    private var navigationListener: PopupBridgeNavigationListener? = null
    private var messageListener: PopupBridgeMessageListener? = null
    private var errorListener: PopupBridgeErrorListener? = null

    /**
     * Create a new instance of [PopupBridgeClient].
     *
     * This will enable JavaScript in your WebView.
     *
     * @param activity The [FragmentActivity] to add the [Fragment] to.
     * @param webView The [WebView] to enable for PopupBridge.
     * @param returnUrlScheme The return url scheme to use for deep linking back into the application.
     * @throws IllegalArgumentException If the activity is not valid or the fragment cannot be added.
     */
    constructor(
        activity: FragmentActivity,
        webView: WebView,
        returnUrlScheme: String
    ) : this(
        activityRef = WeakReference<FragmentActivity>(activity),
        webViewRef = WeakReference<WebView>(webView),
        returnUrlScheme = returnUrlScheme
    )

    init {
        val activity = activityRef.get()
        requireNotNull(activity) { "Activity is null" }

        val webView = webViewRef.get()
        requireNotNull(webView) { "WebView is null" }

        webView.settings.javaScriptEnabled = true
        webView.addJavascriptInterface(this, POPUP_BRIDGE_NAME)
    }

    fun handleReturnToApp(intent: Intent) {
        val pendingRequest = pendingRequestRepository.getPendingRequest()
        if (pendingRequest == null) return

        pendingRequestRepository.clearPendingRequest()

        when (val browserSwitchFinalResult = browserSwitchClient.completeRequest(intent, pendingRequest)) {
            is BrowserSwitchFinalResult.Success -> runSuccessJavaScript(browserSwitchFinalResult.returnUrl)
            is BrowserSwitchFinalResult.Failure -> runCanceledJavaScript()
            is BrowserSwitchFinalResult.NoResult -> runCanceledJavaScript()
        }
    }

    private fun runSuccessJavaScript(returnUri: Uri) {
        if (returnUri.host != POPUP_BRIDGE_URL_HOST) return

        var error: String? = null

        val payLoadJson = JSONObject()
        val queryItems = JSONObject()

        for (queryParam in returnUri.queryParameterNames) {
            try {
                queryItems.put(queryParam, returnUri.getQueryParameter(queryParam))
            } catch (e: JSONException) {
                error = "new Error('Failed to parse query items from return URL. " +
                        e.localizedMessage + "')"
            }
        }

        try {
            payLoadJson.put("path", returnUri.path)
            payLoadJson.put("queryItems", queryItems)
            payLoadJson.put("hash", returnUri.fragment)
        } catch (ignored: JSONException) {
        }

        val successJavascript = String.format(
            (""
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
                    + "}"), error, payLoadJson.toString()
        )

        runJavaScriptInWebView(successJavascript)
    }

    private fun runCanceledJavaScript() {
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

    @get:JavascriptInterface
    val returnUrlPrefix: String
        get() = String.format(
            "%s://%s/",
            returnUrlScheme,
            POPUP_BRIDGE_URL_HOST
        )

    @JavascriptInterface
    fun open(url: String?) {
        val activity = activityRef.get() ?: return
        val browserSwitchOptions = BrowserSwitchOptions()
            .requestCode(REQUEST_CODE)
            .url(url?.toUri())
            .returnUrlScheme(returnUrlScheme)
        val browserSwitchStartResult = browserSwitchClient.start(activity, browserSwitchOptions)
        when (browserSwitchStartResult) {
            is BrowserSwitchStartResult.Started -> {
                pendingRequestRepository.storePendingRequest(browserSwitchStartResult.pendingRequest)
            }

            is BrowserSwitchStartResult.Failure -> {
                errorListener?.onError(browserSwitchStartResult.error)
            }
        }
        navigationListener?.onUrlOpened(url)
    }

    @JavascriptInterface
    fun sendMessage(messageName: String?) {
        messageListener?.onMessageReceived(messageName, null)
    }

    @JavascriptInterface
    fun sendMessage(messageName: String?, data: String?) {
        messageListener?.onMessageReceived(messageName, data)
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

    companion object {
        // NEXT MAJOR VERSION: consider using a `com.braintreepayments...` prefixed request key
        // to prevent shared preferences collisions with other braintree libs that use browser switch
        private const val REQUEST_CODE: Int = 1

        private const val POPUP_BRIDGE_NAME: String = "popupBridge"
        private const val POPUP_BRIDGE_URL_HOST: String = "popupbridgev1"
    }
}