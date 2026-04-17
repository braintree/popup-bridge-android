package com.braintreepayments.api.internal

import android.content.Context
import android.webkit.JavascriptInterface

/**
 * `PopupBridgeJavascriptInterface` is a class that provides JavaScript interface methods to a [android.webkit.WebView].
 *
 * The [android.webkit.WebView] should call [android.webkit.WebView.addJavascriptInterface], passing this class as the
 * first argument. The web page can then use [window.popupBridge] to access:
 * - [isPayPalInstalled] / [isVenmoInstalled] — whether the native apps are installed
 * - [open], [launchApp] — open a URL in browser or native app
 * - [sendMessage] — send messages to the host
 */
internal class PopupBridgeJavascriptInterface(
    private val returnUrlScheme: String,
    private val context: Context,
    private val enablePopupBridgeAppSwitch: Boolean = false,
) {

    var onOpen: ((url: String?) -> Unit)? = null
    var onLaunchApp: ((url: String?) -> Unit)? = null
    var onSendMessage: ((messageName: String?, data: String?) -> Unit)? = null

    /** Exposed to JS as window.popupBridge.isPayPalInstalled. Called by the web page, not by Kotlin. */
    @get:JavascriptInterface
    val isPayPalInstalled: Boolean
        get() = enablePopupBridgeAppSwitch && context.isPayPalInstalled()

    /** Exposed to JS as window.popupBridge.isVenmoInstalled. Called by the web page, not by Kotlin. */
    @get:JavascriptInterface
    val isVenmoInstalled: Boolean
        get() = context.isVenmoInstalled()

    @get:JavascriptInterface
    val returnUrlPrefix: String
        get() = String.format(
            "%s://%s/",
            returnUrlScheme,
            POPUP_BRIDGE_URL_HOST
        )

    @JavascriptInterface
    fun open(url: String?) {
        onOpen?.invoke(url)
    }

    /** Exposed to JS as window.popupBridge.launchApp(url). Called by the web page; triggers [onLaunchApp] (wired in PopupBridgeClient to open the URL in the native app or fallback to browser). */
    @JavascriptInterface
    fun launchApp(url: String?) {
        onLaunchApp?.invoke(url)
    }

    @JavascriptInterface
    fun sendMessage(messageName: String?) {
        onSendMessage?.invoke(messageName, null)
    }

    @JavascriptInterface
    fun sendMessage(messageName: String?, data: String?) {
        onSendMessage?.invoke(messageName, data)
    }

    companion object {
        const val POPUP_BRIDGE_URL_HOST: String = "popupbridgev1"
    }
}
