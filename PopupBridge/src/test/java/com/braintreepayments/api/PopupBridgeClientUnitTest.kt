package com.braintreepayments.api

import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.core.net.toUri
import com.braintreepayments.api.PopupBridgeAnalytics.POPUP_BRIDGE_CANCELED
import com.braintreepayments.api.PopupBridgeAnalytics.POPUP_BRIDGE_FAILED
import com.braintreepayments.api.PopupBridgeAnalytics.POPUP_BRIDGE_STARTED
import com.braintreepayments.api.PopupBridgeAnalytics.POPUP_BRIDGE_SUCCEEDED
import com.braintreepayments.api.internal.AnalyticsClient
import com.braintreepayments.api.internal.AnalyticsParamRepository
import com.braintreepayments.api.internal.PendingRequestRepository
import com.braintreepayments.api.internal.PopupBridgeJavascriptInterface
import com.braintreepayments.api.util.CoroutineTestRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.json.JSONException
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PopupBridgeClientUnitTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    private val testDispatcher = coroutineTestRule.testDispatcher

    private val activityMock: ComponentActivity = mockk(relaxed = true)
    private val webViewMock: WebView = mockk(relaxed = true)
    private val browserSwitchClient: BrowserSwitchClient = mockk(relaxed = true)
    private val popupBridgeWebViewClient: PopupBridgeWebViewClient = mockk(relaxed = true)
    private val pendingRequestRepository: PendingRequestRepository = mockk(relaxed = true)
    private val analyticsClient: AnalyticsClient = mockk(relaxed = true)
    private val popupBridgeJavascriptInterface: PopupBridgeJavascriptInterface = mockk(relaxed = true)
    private val analyticsParamRepository: AnalyticsParamRepository = mockk(relaxed = true)

    private lateinit var subject: PopupBridgeClient

    private val returnUrlScheme = "com.braintreepayments.popupbridgeexample"
    private val pendingRequest = "stored-pending-request"

    private val intent: Intent = mockk(relaxed = true)
    private val runnableSlot = slot<Runnable>()
    private val onOpenSlot = slot<(String?) -> Unit>()
    private val onSendMessageSlot = slot<(String?, String?) -> Unit>()

    private fun initializeClient(
        activity: ComponentActivity = activityMock,
        webView: WebView = webViewMock,
        additionalMocks: () -> Unit = {}
    ) {
        every { webView.post(capture(runnableSlot)) } returns true
        coEvery { pendingRequestRepository.getPendingRequest() } returns pendingRequest
        every { popupBridgeJavascriptInterface.onOpen = capture(onOpenSlot) } returns Unit
        every { popupBridgeJavascriptInterface.onSendMessage = capture(onSendMessageSlot) } returns Unit

        additionalMocks()

        subject = PopupBridgeClient(
            activity = activity,
            webView = webView,
            returnUrlScheme = returnUrlScheme,
            popupBridgeWebViewClient = popupBridgeWebViewClient,
            browserSwitchClient = browserSwitchClient,
            pendingRequestRepository = pendingRequestRepository,
            coroutineScope = TestScope(testDispatcher),
            analyticsClient = analyticsClient,
            analyticsParamRepository = analyticsParamRepository,
            popupBridgeJavascriptInterface = popupBridgeJavascriptInterface,
        )
    }

    @Test
    fun `on init, webView is set up`() {
        initializeClient()

        verify { webViewMock.settings.javaScriptEnabled = true }
        verify { webViewMock.addJavascriptInterface(popupBridgeJavascriptInterface, "popupBridge") }
    }

    @Test
    fun `on init, analyticsParamRepository reset is called`() {
        initializeClient()

        verify { analyticsParamRepository.reset() }
    }

    @Test
    fun `when handleReturnToApp is called multiple times, only one is executed`() = runTest {
        initializeClient()

        subject.handleReturnToApp(intent)
        subject.handleReturnToApp(intent)
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 1) { pendingRequestRepository.getPendingRequest() }
    }

    @Test
    fun `when handleReturnToApp is called with a null pending request, nothing happens`() = runTest {
        initializeClient()
        coEvery { pendingRequestRepository.getPendingRequest() } returns null

        subject.handleReturnToApp(intent)
        testScheduler.advanceUntilIdle()

        verify(exactly = 0) { browserSwitchClient.completeRequest(any(), any()) }
    }

    @Test
    fun `when handleReturnToApp is called, the request in pendingRequestRepository is cleared`() = runTest {
        initializeClient()

        subject.handleReturnToApp(intent)
        testScheduler.advanceUntilIdle()

        coVerify { pendingRequestRepository.clearPendingRequest() }
    }

    @Test
    fun `when handleReturnToApp is called and browser switch succeeds but uri host doesn't match, nothing happens`() =
        runTest {
            val returnUrl = Uri.Builder()
                .scheme("my-custom-url-scheme")
                .authority("unknown-host") // non popup bridge host
                .path("mypath")
                .appendQueryParameter("foo", "bar")
                .appendQueryParameter("baz", "qux")
                .build()

            val browserSwitchFinalResult = mockk<BrowserSwitchFinalResult.Success>()
            every { browserSwitchClient.completeRequest(intent, pendingRequest) } returns browserSwitchFinalResult
            every { browserSwitchFinalResult.returnUrl } returns returnUrl
            initializeClient()

            subject.handleReturnToApp(intent)
            testScheduler.advanceUntilIdle()

            verify(exactly = 0) { webViewMock.post(any()) }
        }

    @Test
    fun `when handleReturnToApp is called and browser switch succeeds, success javascript is run`() = runTest {
        val returnUrl = Uri.Builder()
            .scheme("my-custom-url-scheme")
            .authority("popupbridgev1")
            .path("mypath")
            .appendQueryParameter("first", "first_value")
            .appendQueryParameter("second", "second_value")
            .build()

        val browserSwitchFinalResult = mockk<BrowserSwitchFinalResult.Success>()
        every { browserSwitchClient.completeRequest(intent, pendingRequest) } returns browserSwitchFinalResult
        every { browserSwitchFinalResult.returnUrl } returns returnUrl

        val payloadJson = JSONObject().apply {
            val queryItems = JSONObject().apply {
                put("first", "first_value")
                put("second", "second_value")
            }
            put("path", "/mypath")
            put("queryItems", queryItems)
            put("hash", returnUrl.fragment)
        }
        val expectedJavascriptString = getExpectedSuccessJavascript(null, payloadJson)

        initializeClient()

        subject.handleReturnToApp(intent)
        testScheduler.advanceUntilIdle()
        runnableSlot.captured.run()

        verify {
            webViewMock.evaluateJavascript(withArg { javascriptString ->
                assertEquals(expectedJavascriptString, javascriptString)
            }, null)
        }
    }

    @Test
    fun `when handleReturnToApp is called and browser switch succeeds but throws a JSONException, error is populated`() =
        runTest {
            val exception = JSONException("exception message")
            val returnUrl: Uri = mockk(relaxed = true)
            every { returnUrl.getQueryParameter(any()) } throws exception
            every { returnUrl.host } returns "popupbridgev1"
            every { returnUrl.queryParameterNames } returns setOf("first", "second")

            val browserSwitchFinalResult = mockk<BrowserSwitchFinalResult.Success>()
            every { browserSwitchClient.completeRequest(intent, pendingRequest) } returns browserSwitchFinalResult
            every { browserSwitchFinalResult.returnUrl } returns returnUrl

            initializeClient()

            subject.handleReturnToApp(intent)
            testScheduler.advanceUntilIdle()
            runnableSlot.captured.run()

            verify {
                webViewMock.evaluateJavascript(withArg { javascriptString ->
                    assertTrue(
                        javascriptString.contains(
                            "new Error('Failed to parse query items from return URL. ${exception.localizedMessage}')"
                        )
                    )
                }, null)
            }
        }

    @Test
    fun `when handleReturnToApp is called and browser switch fails, error javascript is run`() = runTest {
        val browserSwitchFinalResult = mockk<BrowserSwitchFinalResult.Failure>()
        every { browserSwitchClient.completeRequest(intent, pendingRequest) } returns browserSwitchFinalResult
        val exception = BrowserSwitchException("error message")
        every { browserSwitchFinalResult.error } returns exception
        initializeClient()

        subject.handleReturnToApp(intent)
        testScheduler.advanceUntilIdle()
        runnableSlot.captured.run()

        verify {
            webViewMock.evaluateJavascript(withArg { javascriptString ->
                    assertTrue(
                        javascriptString.contains(
                            getExpectedFailureJavascript(exception.message)
                        )
                    )
                }, null)
        }
    }

    @Test
    fun `when handleReturnToApp is called and browser switch returns no result, canceled javascript is run`() =
        runTest {
            val browserSwitchFinalResult = mockk<BrowserSwitchFinalResult.NoResult>()
            every { browserSwitchClient.completeRequest(intent, pendingRequest) } returns browserSwitchFinalResult

            initializeClient()

            subject.handleReturnToApp(intent)
            testScheduler.advanceUntilIdle()
            runnableSlot.captured.run()

            verify {
                webViewMock.evaluateJavascript(withArg { javascriptString ->
                    assertEquals(CANCELED_JAVASCRIPT, javascriptString)
                }, null)
            }
        }

    @Test
    fun `when handleReturnToApp is called and browser switch succeeds, POPUP_BRIDGE_SUCCEEDED event is sent`() =
        runTest {
            val returnUrl = Uri.Builder()
                .scheme("my-custom-url-scheme")
                .authority("popupbridgev1")
                .build()

            val browserSwitchFinalResult = mockk<BrowserSwitchFinalResult.Success>()
            every { browserSwitchClient.completeRequest(intent, pendingRequest) } returns browserSwitchFinalResult
            every { browserSwitchFinalResult.returnUrl } returns returnUrl

            initializeClient()

            subject.handleReturnToApp(intent)
            testScheduler.advanceUntilIdle()

            verify { analyticsClient.sendEvent(POPUP_BRIDGE_SUCCEEDED) }
        }

    @Test
    fun `when handleReturnToApp is called and browser switch fails, POPUP_BRIDGE_FAILED event is sent`() = runTest {
        val exception = JSONException("exception message")
        val returnUrl: Uri = mockk(relaxed = true)
        every { returnUrl.getQueryParameter(any()) } throws exception
        every { returnUrl.host } returns "popupbridgev1"
        every { returnUrl.queryParameterNames } returns setOf("param1", "param2")

        val browserSwitchFinalResult = mockk<BrowserSwitchFinalResult.Success>()
        every { browserSwitchClient.completeRequest(intent, pendingRequest) } returns browserSwitchFinalResult
        every { browserSwitchFinalResult.returnUrl } returns returnUrl

        initializeClient()

        subject.handleReturnToApp(intent)
        testScheduler.advanceUntilIdle()

        verify { analyticsClient.sendEvent(POPUP_BRIDGE_FAILED) }
    }

    @Test
    fun `when handleReturnToApp is called and browser switch returns no result, POPUP_BRIDGE_CANCELED event is sent`() =
        runTest {
            val browserSwitchFinalResult = mockk<BrowserSwitchFinalResult.NoResult>()
            every { browserSwitchClient.completeRequest(intent, pendingRequest) } returns browserSwitchFinalResult

            initializeClient()

            subject.handleReturnToApp(intent)
            testScheduler.advanceUntilIdle()

            verify { analyticsClient.sendEvent(POPUP_BRIDGE_CANCELED) }
        }

    @Test
    fun `when popupBridgeJavascriptInterface onOpen is called, analyticsClient sends POPUP_BRIDGE_STARTED event`() {
        initializeClient()

        onOpenSlot.captured.invoke("https://example.com")

        verify { analyticsClient.sendEvent(POPUP_BRIDGE_STARTED) }
    }

    @Test
    fun `when popupBridgeJavascriptInterface onOpen is called, the correct BrowserSwitchOptions are created`() {
        initializeClient()

        onOpenSlot.captured.invoke("https://example.com")

        verify(exactly = 1) {
            browserSwitchClient.start(activityMock, withArg { browserSwitchOptions ->
                assertEquals("https://example.com".toUri(), browserSwitchOptions.url)
                assertEquals(1, browserSwitchOptions.requestCode)
                assertEquals(returnUrlScheme, browserSwitchOptions.returnUrlScheme)
            })
        }
    }

    @Test
    fun `when open is called with BrowserSwitchStartResult Started, the pendingRequest is stored in pendingRequestRepository`() =
        runTest {
            every { browserSwitchClient.start(activityMock, any()) } returns
                    BrowserSwitchStartResult.Started(pendingRequest)
            initializeClient()

            onOpenSlot.captured.invoke("https://example.com")
            testScheduler.advanceUntilIdle()

            coVerify { pendingRequestRepository.storePendingRequest(pendingRequest) }
        }

    @Test
    fun `when open is called with BrowserSwitchStartResult Failure, the error listener is invoked`() {
        val exception = Exception()
        every { browserSwitchClient.start(activityMock, any()) } returns
                BrowserSwitchStartResult.Failure(exception)
        val errorListener: PopupBridgeErrorListener = mockk(relaxed = true)
        initializeClient()

        subject.setErrorListener(errorListener)
        onOpenSlot.captured.invoke("https://example.com")

        verify { errorListener.onError(exception) }
    }

    @Test
    fun `when open is called, the navigation listener is invoked`() {
        val url = "https://example.com"
        val navigationListener: PopupBridgeNavigationListener = mockk(relaxed = true)
        initializeClient()

        subject.setNavigationListener(navigationListener)
        onOpenSlot.captured.invoke(url)

        verify { navigationListener.onUrlOpened(url) }
    }

    @Test
    fun `when sendMessage is called, the message listener is invoked`() {
        val messageName = "messageName"
        val data = "data"
        val messageListener: PopupBridgeMessageListener = mockk(relaxed = true)
        initializeClient()

        subject.setMessageListener(messageListener)
        onSendMessageSlot.captured.invoke(messageName, data)

        verify { messageListener.onMessageReceived(messageName, data) }
    }

    @Test
    fun `when sendMessage without data is called, the message listener is invoked`() {
        val messageName = "messageName"
        val messageListener: PopupBridgeMessageListener = mockk(relaxed = true)
        initializeClient()

        subject.setMessageListener(messageListener)
        onSendMessageSlot.captured.invoke(messageName, null)

        verify { messageListener.onMessageReceived(messageName, null) }
    }

    private fun getExpectedSuccessJavascript(error: String?, payload: JSONObject): String {
        return String.format(
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
                    + "}"), error, payload.toString()
        )
    }

    private fun getExpectedFailureJavascript(error: String?): String {
        return String.format(
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
                + "}"), error, null
        )
    }

    companion object {
        private const val CANCELED_JAVASCRIPT = (""
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
    }
}
