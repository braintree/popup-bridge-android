package com.braintreepayments.api

import android.webkit.WebView
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.BeforeTest
import kotlin.test.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PopupBridgeWebViewClientTest {

    private val sut = PopupBridgeWebViewClient()
    private val webView = mockk<WebView>(relaxed = true)

    @BeforeTest
    fun setup() {
    }

    @Test
    fun `onPageFinished calls super and does not inject installed state`() {
        sut.onPageFinished(webView, "https://example.com")

        verify(exactly = 0) { webView.evaluateJavascript(any(), any()) }
    }
}
