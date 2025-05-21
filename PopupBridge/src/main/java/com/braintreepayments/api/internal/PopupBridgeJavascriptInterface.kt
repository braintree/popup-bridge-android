package com.braintreepayments.api.internal

import android.webkit.JavascriptInterface

/**
 * `PopupBridgeJavascriptInterface` is a class that provides JavaScript interface methods to a [android.webkit.WebView].
 *
 * The [android.webkit.WebView] should call [android.webkit.WebView.addJavascriptInterface], passing this class as the
 * first argument.
 */
internal class PopupBridgeJavascriptInterface(
    private val returnUrlScheme: String,
) {

    var onOpen: ((url: String?) -> Unit)? = null
    var onSendMessage: ((messageName: String?, data: String?) -> Unit)? = null

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
