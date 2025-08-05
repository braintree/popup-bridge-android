package com.braintreepayments.api

import android.webkit.WebView
import android.webkit.WebViewClient
import com.braintreepayments.api.internal.isVenmoInstalled
import org.jetbrains.annotations.VisibleForTesting

class PopupBridgeWebViewClient : WebViewClient() {

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        setVenmoInstalled(view, view?.context?.isVenmoInstalled() == true)
    }

    @VisibleForTesting
    internal fun setVenmoInstalled(view: WebView?, isVenmoInstalled: Boolean) {
        runJavaScriptInWebView(view,
            ""
                + "function setVenmoInstalled() {"
                + "    window.popupBridge.isVenmoInstalled = ${isVenmoInstalled};"
                + "}"
                + ""
                + "if (document.readyState === 'complete') {"
                + "  setVenmoInstalled();"
                + "} else {"
                + "  window.addEventListener('load', function () {"
                + "    setVenmoInstalled();"
                + "  });"
                + "}"
        )
    }

    private fun runJavaScriptInWebView(webView: WebView?, script: String) {
        webView?.evaluateJavascript(script, null)
//        webView?.post(
//            Runnable { webView.evaluateJavascript(script, null) }
//        )
    }
}
