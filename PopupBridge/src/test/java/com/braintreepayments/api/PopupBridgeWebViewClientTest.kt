package com.braintreepayments.api

import android.os.Message
import android.view.KeyEvent
import android.webkit.ClientCertRequest
import android.webkit.HttpAuthHandler
import android.webkit.SslErrorHandler
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.braintreepayments.api.internal.isVenmoInstalled
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PopupBridgeWebViewClientTest {

    private val sut = PopupBridgeWebViewClient()
    private val webView = mockk<WebView>(relaxed = true)

    @BeforeTest
    fun setup() {
        mockkStatic("com.braintreepayments.api.internal.AppInstalledChecksKt")

        every { webView.post(any()) } answers {
            val runnable = firstArg<Runnable>()
            runnable.run()
            true
        }
    }

    @Test
    fun `on init, when venmo installed, isVenmoInstalled is set to true on the popupBridgeJavascriptInterface`() =
        runTest {
        every { webView.context.isVenmoInstalled() } returns true

        sut.onPageFinished(webView, "https://example.com")

        verify {
            webView.evaluateJavascript(withArg { javascriptString ->
                assertEquals(getExpectedVenmoInstalledJavascript(true), javascriptString)
            }, null)
        }

        unmockkAll()
    }

    @Test
    fun `on init, when venmo is not installed, isVenmoInstalled is false on javascriptInterface`() =
        runTest {
        every { webView.context.isVenmoInstalled() } returns false

        sut.onPageFinished(webView, "https://example.com")

        verify {
            webView.evaluateJavascript(withArg { javascriptString ->
                assertEquals(getExpectedVenmoInstalledJavascript(false), javascriptString)
            }, null)
        }

        unmockkAll()
    }

    @Test
    fun `onPageFinished calls delegate when provided`() = runTest {
        val delegate = mockk<WebViewClient>(relaxed = true)
        val sutWithDelegate = PopupBridgeWebViewClient(delegate)
        every { webView.context.isVenmoInstalled() } returns false

        sutWithDelegate.onPageFinished(webView, "https://example.com")

        verify { delegate.onPageFinished(webView, "https://example.com") }
        unmockkAll()
    }

    @Suppress("DEPRECATION")
    @Test
    fun `shouldOverrideUrlLoading with string url delegates to delegate when provided`() {
        val delegate = mockk<WebViewClient>(relaxed = true)
        val sutWithDelegate = PopupBridgeWebViewClient(delegate)
        every { delegate.shouldOverrideUrlLoading(webView, "https://example.com") } returns true

        val result = sutWithDelegate.shouldOverrideUrlLoading(webView, "https://example.com")

        assertTrue(result)
        verify { delegate.shouldOverrideUrlLoading(webView, "https://example.com") }
    }

    @Suppress("DEPRECATION")
    @Test
    fun `shouldOverrideUrlLoading with string url returns false when no delegate`() {
        val result = sut.shouldOverrideUrlLoading(webView, "https://example.com")

        assertFalse(result)
    }

    @Test
    fun `shouldOverrideUrlLoading with request delegates to delegate when provided`() {
        val delegate = mockk<WebViewClient>(relaxed = true)
        val sutWithDelegate = PopupBridgeWebViewClient(delegate)
        val request = mockk<WebResourceRequest>(relaxed = true)
        every { delegate.shouldOverrideUrlLoading(webView, request) } returns true

        val result = sutWithDelegate.shouldOverrideUrlLoading(webView, request)

        assertTrue(result)
        verify { delegate.shouldOverrideUrlLoading(webView, request) }
    }

    @Test
    fun `shouldOverrideUrlLoading with request returns false when no delegate`() {
        val request = mockk<WebResourceRequest>(relaxed = true)

        val result = sut.shouldOverrideUrlLoading(webView, request)

        assertFalse(result)
    }

    @Test
    fun `onPageStarted delegates when delegate provided`() {
        val delegate = mockk<WebViewClient>(relaxed = true)
        val sutWithDelegate = PopupBridgeWebViewClient(delegate)

        sutWithDelegate.onPageStarted(webView, "https://example.com", null)

        verify { delegate.onPageStarted(webView, "https://example.com", null) }
    }

    @Test
    fun `onPageStarted does not throw when no delegate`() {
        sut.onPageStarted(webView, "https://example.com", null)
    }

    @Test
    fun `onLoadResource delegates when delegate provided`() {
        val delegate = mockk<WebViewClient>(relaxed = true)
        val sutWithDelegate = PopupBridgeWebViewClient(delegate)

        sutWithDelegate.onLoadResource(webView, "https://example.com/resource")

        verify { delegate.onLoadResource(webView, "https://example.com/resource") }
    }

    @Test
    fun `onLoadResource does not throw when no delegate`() {
        sut.onLoadResource(webView, "https://example.com/resource")
    }

    @Test
    fun `onPageCommitVisible delegates when delegate provided`() {
        val delegate = mockk<WebViewClient>(relaxed = true)
        val sutWithDelegate = PopupBridgeWebViewClient(delegate)

        sutWithDelegate.onPageCommitVisible(webView, "https://example.com")

        verify { delegate.onPageCommitVisible(webView, "https://example.com") }
    }

    @Test
    fun `onPageCommitVisible does not throw when no delegate`() {
        sut.onPageCommitVisible(webView, "https://example.com")
    }

    @Test
    fun `onReceivedError delegates when delegate provided`() {
        val delegate = mockk<WebViewClient>(relaxed = true)
        val sutWithDelegate = PopupBridgeWebViewClient(delegate)
        val request = mockk<WebResourceRequest>(relaxed = true)

        sutWithDelegate.onReceivedError(webView, request, null)

        verify { delegate.onReceivedError(webView, request, null) }
    }

    @Test
    fun `onReceivedError does not throw when no delegate`() {
        val request = mockk<WebResourceRequest>(relaxed = true)
        sut.onReceivedError(webView, request, null)
    }

    @Test
    fun `onReceivedHttpError delegates when delegate provided`() {
        val delegate = mockk<WebViewClient>(relaxed = true)
        val sutWithDelegate = PopupBridgeWebViewClient(delegate)
        val request = mockk<WebResourceRequest>(relaxed = true)
        val errorResponse = mockk<WebResourceResponse>(relaxed = true)

        sutWithDelegate.onReceivedHttpError(webView, request, errorResponse)

        verify { delegate.onReceivedHttpError(webView, request, errorResponse) }
    }

    @Test
    fun `onReceivedHttpError does not throw when no delegate`() {
        sut.onReceivedHttpError(webView, null, null)
    }

    @Test
    fun `onFormResubmission delegates when delegate provided`() {
        val delegate = mockk<WebViewClient>(relaxed = true)
        val sutWithDelegate = PopupBridgeWebViewClient(delegate)
        val dontResend = mockk<Message>(relaxed = true)
        val resend = mockk<Message>(relaxed = true)

        sutWithDelegate.onFormResubmission(webView, dontResend, resend)

        verify { delegate.onFormResubmission(webView, dontResend, resend) }
    }

    @Test
    fun `onFormResubmission does not throw when no delegate`() {
        val dontResend = mockk<Message>(relaxed = true)
        sut.onFormResubmission(webView, dontResend, null)
    }

    @Test
    fun `doUpdateVisitedHistory delegates when delegate provided`() {
        val delegate = mockk<WebViewClient>(relaxed = true)
        val sutWithDelegate = PopupBridgeWebViewClient(delegate)

        sutWithDelegate.doUpdateVisitedHistory(webView, "https://example.com", false)

        verify { delegate.doUpdateVisitedHistory(webView, "https://example.com", false) }
    }

    @Test
    fun `doUpdateVisitedHistory does not throw when no delegate`() {
        sut.doUpdateVisitedHistory(webView, "https://example.com", false)
    }

    @Test
    fun `onReceivedSslError delegates when delegate provided`() {
        val delegate = mockk<WebViewClient>(relaxed = true)
        val sutWithDelegate = PopupBridgeWebViewClient(delegate)
        val handler = mockk<SslErrorHandler>(relaxed = true)

        sutWithDelegate.onReceivedSslError(webView, handler, null)

        verify { delegate.onReceivedSslError(webView, handler, null) }
    }

    @Test
    fun `onReceivedSslError does not throw when no delegate`() {
        val handler = mockk<SslErrorHandler>(relaxed = true)
        sut.onReceivedSslError(webView, handler, null)
    }

    @Test
    fun `onReceivedClientCertRequest delegates when delegate provided`() {
        val delegate = mockk<WebViewClient>(relaxed = true)
        val sutWithDelegate = PopupBridgeWebViewClient(delegate)

        sutWithDelegate.onReceivedClientCertRequest(webView, null)

        verify { delegate.onReceivedClientCertRequest(webView, null) }
    }

    @Test
    fun `onReceivedClientCertRequest does not throw when no delegate`() {
        val request = mockk<ClientCertRequest>(relaxed = true)
        sut.onReceivedClientCertRequest(webView, request)
    }

    @Test
    fun `onReceivedHttpAuthRequest delegates when delegate provided`() {
        val delegate = mockk<WebViewClient>(relaxed = true)
        val sutWithDelegate = PopupBridgeWebViewClient(delegate)
        val handler = mockk<HttpAuthHandler>(relaxed = true)

        sutWithDelegate.onReceivedHttpAuthRequest(webView, handler, "example.com", "realm")

        verify { delegate.onReceivedHttpAuthRequest(webView, handler, "example.com", "realm") }
    }

    @Test
    fun `onReceivedHttpAuthRequest does not throw when no delegate`() {
        val handler = mockk<HttpAuthHandler>(relaxed = true)
        sut.onReceivedHttpAuthRequest(webView, handler, "example.com", "realm")
    }

    @Suppress("DEPRECATION")
    @Test
    fun `shouldInterceptRequest with string url delegates to delegate when provided`() {
        val delegate = mockk<WebViewClient>(relaxed = true)
        val sutWithDelegate = PopupBridgeWebViewClient(delegate)
        val response = mockk<WebResourceResponse>(relaxed = true)
        every { delegate.shouldInterceptRequest(webView, "https://example.com") } returns response

        val result = sutWithDelegate.shouldInterceptRequest(webView, "https://example.com")

        assertEquals(response, result)
        verify { delegate.shouldInterceptRequest(webView, "https://example.com") }
    }

    @Suppress("DEPRECATION")
    @Test
    fun `shouldInterceptRequest with string url returns null when no delegate`() {
        val result = sut.shouldInterceptRequest(webView, "https://example.com")

        assertNull(result)
    }

    @Test
    fun `shouldInterceptRequest with request delegates to delegate when provided`() {
        val delegate = mockk<WebViewClient>(relaxed = true)
        val sutWithDelegate = PopupBridgeWebViewClient(delegate)
        val request = mockk<WebResourceRequest>(relaxed = true)
        val response = mockk<WebResourceResponse>(relaxed = true)
        every { delegate.shouldInterceptRequest(webView, request) } returns response

        val result = sutWithDelegate.shouldInterceptRequest(webView, request)

        assertEquals(response, result)
        verify { delegate.shouldInterceptRequest(webView, request) }
    }

    @Test
    fun `shouldInterceptRequest with request returns null when no delegate`() {
        val request = mockk<WebResourceRequest>(relaxed = true)

        val result = sut.shouldInterceptRequest(webView, request)

        assertNull(result)
    }

    @Test
    fun `onUnhandledKeyEvent delegates when delegate provided`() {
        val delegate = mockk<WebViewClient>(relaxed = true)
        val sutWithDelegate = PopupBridgeWebViewClient(delegate)
        val event = mockk<KeyEvent>(relaxed = true)

        sutWithDelegate.onUnhandledKeyEvent(webView, event)

        verify { delegate.onUnhandledKeyEvent(webView, event) }
    }

    @Test
    fun `onUnhandledKeyEvent does not throw when no delegate`() {
        val event = mockk<KeyEvent>(relaxed = true)

        sut.onUnhandledKeyEvent(webView, event)
    }

    @Test
    fun `onScaleChanged delegates when delegate provided`() {
        val delegate = mockk<WebViewClient>(relaxed = true)
        val sutWithDelegate = PopupBridgeWebViewClient(delegate)

        sutWithDelegate.onScaleChanged(webView, 1.0f, 2.0f)

        verify { delegate.onScaleChanged(webView, 1.0f, 2.0f) }
    }

    @Test
    fun `onScaleChanged does not throw when no delegate`() {
        sut.onScaleChanged(webView, 1.0f, 2.0f)
    }

    @Test
    fun `onReceivedLoginRequest delegates when delegate provided`() {
        val delegate = mockk<WebViewClient>(relaxed = true)
        val sutWithDelegate = PopupBridgeWebViewClient(delegate)

        sutWithDelegate.onReceivedLoginRequest(webView, "realm", "account", "args")

        verify { delegate.onReceivedLoginRequest(webView, "realm", "account", "args") }
    }

    @Test
    fun `onReceivedLoginRequest does not throw when no delegate`() {
        sut.onReceivedLoginRequest(webView, "realm", "account", "args")
    }

    private fun getExpectedVenmoInstalledJavascript(isVenmoInstalled: Boolean): String {
        return String.format(
            ("" +
                "function setVenmoInstalled() {" +
                "    window.popupBridge.isVenmoInstalled = %s;" +
                "}" +
                "" +
                "if (document.readyState === 'complete') {" +
                "  setVenmoInstalled();" +
                "} else {" +
                "  window.addEventListener('load', function () {" +
                "    setVenmoInstalled();" +
                "  });" +
                "}"), isVenmoInstalled
        )
    }
}
