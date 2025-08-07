package com.braintreepayments.api

import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Build
import android.os.Message
import android.view.KeyEvent
import android.webkit.ClientCertRequest
import android.webkit.HttpAuthHandler
import android.webkit.SslErrorHandler
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import com.braintreepayments.api.internal.isVenmoInstalled

class PopupBridgeWebViewClient(
    private val delegate: WebViewClient? = null
) : WebViewClient() {

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        setVenmoInstalled(view, view?.context?.isVenmoInstalled() == true)
        delegate?.onPageFinished(view, url)
    }

    @Deprecated("Deprecated in [android.webkit.WebViewClient]")
    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        return delegate?.shouldOverrideUrlLoading(view, url) ?: super.shouldOverrideUrlLoading(view, url)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        return delegate?.shouldOverrideUrlLoading(view, request) ?: super.shouldOverrideUrlLoading(view, request)
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        delegate?.onPageStarted(view, url, favicon) ?: super.onPageStarted(view, url, favicon)
    }

    override fun onLoadResource(view: WebView?, url: String?) {
        delegate?.onLoadResource(view, url) ?: super.onLoadResource(view, url)
    }

    override fun onPageCommitVisible(view: WebView?, url: String?) {
        delegate?.onPageCommitVisible(view, url) ?: super.onPageCommitVisible(view, url)
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: android.webkit.WebResourceError?
    ) {
        delegate?.onReceivedError(view, request, error) ?: super.onReceivedError(view, request, error)
    }

    override fun onReceivedHttpError(
        view: WebView?,
        request: WebResourceRequest?,
        errorResponse: WebResourceResponse?
    ) {
        delegate?.onReceivedHttpError(view, request, errorResponse)
            ?: super.onReceivedHttpError(view, request, errorResponse)
    }

    override fun onFormResubmission(view: WebView?, dontResend: Message?, resend: Message?) {
        delegate?.onFormResubmission(view, dontResend, resend) ?: super.onFormResubmission(view, dontResend, resend)
    }

    override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
        delegate?.doUpdateVisitedHistory(view, url, isReload) ?: super.doUpdateVisitedHistory(view, url, isReload)
    }

    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
        delegate?.onReceivedSslError(view, handler, error) ?: super.onReceivedSslError(view, handler, error)
    }

    override fun onReceivedClientCertRequest(view: WebView?, request: ClientCertRequest?) {
        delegate?.onReceivedClientCertRequest(view, request) ?: super.onReceivedClientCertRequest(view, request)
    }

    override fun onReceivedHttpAuthRequest(view: WebView?, handler: HttpAuthHandler?, host: String?, realm: String?) {
        delegate?.onReceivedHttpAuthRequest(view, handler, host, realm)
            ?: super.onReceivedHttpAuthRequest(view, handler, host, realm)
    }

    @Deprecated("Deprecated in [android.webkit.WebViewClient]")
    override fun shouldInterceptRequest(view: WebView?, url: String?): WebResourceResponse? {
        return delegate?.shouldInterceptRequest(view, url) ?: super.shouldInterceptRequest(view, url)
    }

    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
        return delegate?.shouldInterceptRequest(view, request) ?: super.shouldInterceptRequest(view, request)
    }

    override fun onUnhandledKeyEvent(view: WebView?, event: KeyEvent?) {
        delegate?.onUnhandledKeyEvent(view, event) ?: super.onUnhandledKeyEvent(view, event)
    }

    override fun onScaleChanged(view: WebView?, oldScale: Float, newScale: Float) {
        delegate?.onScaleChanged(view, oldScale, newScale) ?: super.onScaleChanged(view, oldScale, newScale)
    }

    override fun onReceivedLoginRequest(view: WebView?, realm: String?, account: String?, args: String?) {
        delegate?.onReceivedLoginRequest(view, realm, account, args)
            ?: super.onReceivedLoginRequest(view, realm, account, args)
    }

    private fun setVenmoInstalled(view: WebView?, isVenmoInstalled: Boolean) {
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
        webView?.post(
            Runnable { webView.evaluateJavascript(script, null) }
        )
    }
}
