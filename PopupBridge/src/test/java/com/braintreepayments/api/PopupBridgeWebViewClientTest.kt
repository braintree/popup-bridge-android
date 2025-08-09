package com.braintreepayments.api

import android.webkit.WebView
import com.braintreepayments.api.internal.isVenmoInstalled
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import kotlin.test.BeforeTest
import kotlin.test.Test
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
    fun `on init, when venmo installed, isVenmoInstalled is set to true on the popupBridgeJavascriptInterface`() = runTest {
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
    fun `on init, when venmo is not installed, isVenmoInstalled is set to false on the popupBridgeJavascriptInterface`() = runTest {
        every { webView.context.isVenmoInstalled() } returns false

        sut.onPageFinished(webView, "https://example.com")

        verify {
            webView.evaluateJavascript(withArg { javascriptString ->
                assertEquals(getExpectedVenmoInstalledJavascript(false), javascriptString)
            }, null)
        }

        unmockkAll()
    }

    private fun getExpectedVenmoInstalledJavascript(isVenmoInstalled: Boolean): String {
        return String.format(
            (""
                + "function setVenmoInstalled() {"
                + "    window.popupBridge.isVenmoInstalled = %s;"
                + "}"
                + ""
                + "if (document.readyState === 'complete') {"
                + "  setVenmoInstalled();"
                + "} else {"
                + "  window.addEventListener('load', function () {"
                + "    setVenmoInstalled();"
                + "  });"
                + "}"), isVenmoInstalled
        )
    }
}
