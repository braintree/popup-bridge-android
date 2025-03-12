package com.braintreepayments.api

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.annotation.VisibleForTesting
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import org.json.JSONException
import org.json.JSONObject
import java.lang.ref.WeakReference

class PopupBridgeClient @SuppressLint("SetJavaScriptEnabled") @VisibleForTesting internal constructor(
    private val activityRef: WeakReference<FragmentActivity>,
    private val webViewRef: WeakReference<WebView>,
    private val returnUrlScheme: String,
    private val browserSwitchClient: BrowserSwitchClient = BrowserSwitchClient(),
    popupBridgeLifecycleObserver: PopupBridgeLifecycleObserver = PopupBridgeLifecycleObserver(browserSwitchClient)
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
    constructor(activity: FragmentActivity, webView: WebView, returnUrlScheme: String) : this(
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

        popupBridgeLifecycleObserver.onBrowserSwitchResult = { result -> onBrowserSwitchResult(result) }
        activity.lifecycle.addObserver(popupBridgeLifecycleObserver)
    }

    private fun runJavaScriptInWebView(script: String) {
        val webView = webViewRef.get() ?: return
        webView.post(Runnable {
            val webViewInternal = webViewRef.get() ?: return@Runnable
            webViewInternal.evaluateJavascript(script, null)
        })
    }

    fun getBrowserSwitchResult(activity: FragmentActivity): BrowserSwitchResult {
        return browserSwitchClient.getResult(activity)
    }

    fun deliverBrowserSwitchResult(activity: FragmentActivity): BrowserSwitchResult {
        return browserSwitchClient.deliverResult(activity)
    }

    fun onBrowserSwitchResult(result: BrowserSwitchResult) {
        var error: String? = null
        var payload: String? = null

        val returnUri = result.deepLinkUrl
        if (result.status == BrowserSwitchStatus.CANCELED) {
            runJavaScriptInWebView(
                (""
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
                        + "}")
            )
            return
        } else if (result.status == BrowserSwitchStatus.SUCCESS) {
            if (returnUri == null || returnUri.host != POPUP_BRIDGE_URL_HOST) {
                return
            }

            val json = JSONObject()
            val queryItems = JSONObject()

            val queryParams = returnUri.queryParameterNames
            if (queryParams.isNotEmpty()) {
                for (queryParam in queryParams) {
                    try {
                        queryItems.put(queryParam, returnUri.getQueryParameter(queryParam))
                    } catch (e: JSONException) {
                        error = "new Error('Failed to parse query items from return URL. " +
                                e.localizedMessage + "')"
                    }
                }
            }

            try {
                json.put("path", returnUri.path)
                json.put("queryItems", queryItems)
                json.put("hash", returnUri.fragment)
            } catch (ignored: JSONException) {
            }

            payload = json.toString()
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
                + "}"), error, payload
        )

        runJavaScriptInWebView(successJavascript)
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
        try {
            browserSwitchClient.start(activity, browserSwitchOptions)
        } catch (e: Exception) {
            errorListener?.onError(e)
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
        const val REQUEST_CODE: Int = 1

        const val POPUP_BRIDGE_NAME: String = "popupBridge"
        const val POPUP_BRIDGE_URL_HOST: String = "popupbridgev1"
    }
}
