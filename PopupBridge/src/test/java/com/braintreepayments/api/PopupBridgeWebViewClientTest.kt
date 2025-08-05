package com.braintreepayments.api

import android.content.Context
import android.content.pm.ApplicationInfo
import android.webkit.WebView
import com.braintreepayments.api.internal.isVenmoInstalled
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PopupBridgeWebViewClientTest {

    private lateinit var sut: PopupBridgeWebViewClient
    private val context = mockk<Context>(relaxed = true)
    private val view = mockk<WebView>(relaxed = true)
    private val url = "some-url"

    @BeforeTest
    fun setup() {
        sut = PopupBridgeWebViewClient()
        mockkStatic("com.braintreepayments.api.internal.AppInstalledChecksKt")
        every { view.context } returns context
        every { context.applicationInfo } returns mockk<ApplicationInfo>(relaxed = true)
    }

    @AfterTest
    fun cleanup() {
        // Unmock static methods to avoid side effects in other tests
        clearAllMocks()
    }

    @Test
    fun `test invocation of onPageFinished calls setVenmoInstalled - venmo installed`() {
        every { view.context.isVenmoInstalled() } returns true

        sut.onPageFinished(view, url)

        verify { sut.setVenmoInstalled(view, true) }
    }

    @Test
    fun `test invocation of onPageFinished calls setVenmoInstalled - venmo not installed`() {
        every { view.context.isVenmoInstalled() } returns false

        sut.onPageFinished(view, url)

        verify { sut.setVenmoInstalled(view, false) }
    }
}
