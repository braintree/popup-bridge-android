package com.braintreepayments.api

import android.content.Intent
import android.net.Uri
import android.os.Looper
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.core.net.toUri
import com.braintreepayments.api.PopupBridgeAnalytics.POPUP_BRIDGE_APP_DETECTED
import com.braintreepayments.api.PopupBridgeAnalytics.POPUP_BRIDGE_APP_LAUNCHED
import com.braintreepayments.api.PopupBridgeAnalytics.POPUP_BRIDGE_APP_LAUNCH_FAILED
import com.braintreepayments.api.PopupBridgeAnalytics.POPUP_BRIDGE_APP_SWITCH_RETURNED
import com.braintreepayments.api.PopupBridgeAnalytics.POPUP_BRIDGE_CANCELED
import com.braintreepayments.api.PopupBridgeAnalytics.POPUP_BRIDGE_FAILED
import com.braintreepayments.api.PopupBridgeAnalytics.POPUP_BRIDGE_STARTED
import com.braintreepayments.api.PopupBridgeAnalytics.POPUP_BRIDGE_SUCCEEDED
import com.braintreepayments.api.internal.AnalyticsClient
import com.braintreepayments.api.internal.AnalyticsParamRepository
import com.braintreepayments.api.internal.PendingRequestRepository
import com.braintreepayments.api.internal.PopupBridgeJavascriptInterface
import com.braintreepayments.api.internal.PopupBridgeJavascriptInterface.Companion.POPUP_BRIDGE_URL_HOST
import com.braintreepayments.api.internal.isPayPalInstalled
import com.braintreepayments.api.util.CoroutineTestRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
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
import org.robolectric.Shadows

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
    private val onLaunchAppSlot = slot<(String?) -> Unit>()
    private val onSendMessageSlot = slot<(String?, String?) -> Unit>()

    private fun initializeClient(
        activity: ComponentActivity = activityMock,
        webView: WebView = webViewMock,
        enablePopupBridgeAppSwitch: Boolean = false,
        additionalMocks: () -> Unit = {}
    ) {
        every { webView.post(capture(runnableSlot)) } returns true
        coEvery { pendingRequestRepository.getPendingRequest() } returns pendingRequest
        every { popupBridgeJavascriptInterface.onOpen = capture(onOpenSlot) } returns Unit
        every { popupBridgeJavascriptInterface.onLaunchApp = capture(onLaunchAppSlot) } returns Unit
        every { popupBridgeJavascriptInterface.onSendMessage = capture(onSendMessageSlot) } returns Unit

        additionalMocks()

        subject = PopupBridgeClient(
            activity = activity,
            webView = webView,
            returnUrlScheme = returnUrlScheme,
            popupBridgeWebViewClient = popupBridgeWebViewClient,
            browserSwitchClient = browserSwitchClient,
            enablePopupBridgeAppSwitch = enablePopupBridgeAppSwitch,
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

    // region App switch / launchApp tests

    @Test
    fun `on init when PayPal is installed POPUP_BRIDGE_APP_DETECTED is sent`() {
        mockkStatic("com.braintreepayments.api.internal.AppInstalledChecksKt")
        every { any<android.content.Context>().isPayPalInstalled() } returns true

        initializeClient(enablePopupBridgeAppSwitch = true)

        verify { analyticsClient.sendEvent(POPUP_BRIDGE_APP_DETECTED) }

        unmockkAll()
    }

    @Test
    fun `on init onLaunchApp callback is wired`() {
        initializeClient()

        // Invoking the callback runs launchApp (posts to main handler); no throw
        onLaunchAppSlot.captured.invoke("https://www.paypal.com/checkout")
    }

    @Test
    fun `when handleReturnToApp is called with app switch intent path browser switch path is not used`() =
        runTest {
            val appSwitchReturnUri = Uri.Builder()
                .scheme(returnUrlScheme)
                .authority(POPUP_BRIDGE_URL_HOST)
                .path("/onApprove")
                .build()
            every { intent.data } returns appSwitchReturnUri

            initializeClient()

            subject.handleReturnToApp(intent)
            testScheduler.advanceUntilIdle()

            coVerify(exactly = 0) { pendingRequestRepository.getPendingRequest() }
            verify(exactly = 0) { browserSwitchClient.completeRequest(any(), any()) }
        }

    @Test
    fun `when handleReturnToApp is called with popup bridge host and query params browser switch path is used`() =
        runTest {
            val browserSwitchReturnUri = Uri.Builder()
                .scheme(returnUrlScheme)
                .authority(POPUP_BRIDGE_URL_HOST)
                .appendQueryParameter("token", "7GJ8435627393170V")
                .appendQueryParameter("PayerID", "7WSWGN7G2SP2Y")
                .appendQueryParameter("opType", "payment")
                .appendQueryParameter("paymentId", "7GJ8435627393170V")
                .build()
            val browserSwitchFinalResult = mockk<BrowserSwitchFinalResult.Success>()
            every { intent.data } returns browserSwitchReturnUri
            every { browserSwitchClient.completeRequest(intent, pendingRequest) } returns browserSwitchFinalResult
            every { browserSwitchFinalResult.returnUrl } returns browserSwitchReturnUri

            initializeClient()

            subject.handleReturnToApp(intent)
            testScheduler.advanceUntilIdle()
            runnableSlot.captured.run()

            coVerify(atLeast = 1) { pendingRequestRepository.getPendingRequest() }
            verify { browserSwitchClient.completeRequest(intent, pendingRequest) }
            verify(exactly = 0) { analyticsClient.sendEvent(POPUP_BRIDGE_APP_SWITCH_RETURNED) }
            verify {
                webViewMock.evaluateJavascript(withArg { script ->
                    assertTrue(script.contains("\"opType\":\"payment\""))
                    assertTrue(script.contains("\"PayerID\":\"7WSWGN7G2SP2Y\""))
                }, null)
            }
        }

    @Test
    fun `when handleReturnToApp is called with app switch intent and expectingAppSwitchReturn path contains onCancel runs canceled JS and sends APP_SWITCH_RETURNED`() =
        runTest {
            val appSwitchReturnUri = Uri.Builder()
                .scheme(returnUrlScheme)
                .authority(POPUP_BRIDGE_URL_HOST)
                .path("/onCancel")
                .build()
            every { intent.data } returns appSwitchReturnUri

            initializeClient()

            setPrivateExpectingAppSwitchReturn(subject, true)

            subject.handleReturnToApp(intent)
            testScheduler.advanceUntilIdle()
            runnableSlot.captured.run()

            verify { analyticsClient.sendEvent(POPUP_BRIDGE_APP_SWITCH_RETURNED) }
            verify {
                webViewMock.evaluateJavascript(withArg { script ->
                    assertTrue(script.contains("notifyCanceled()") && script.contains("onCancel"))
                }, null)
            }
        }

    @Test
    fun `when handleReturnToApp is called with app switch intent and path is cancel segment runs canceled JS`() =
        runTest {
            val appSwitchReturnUri = Uri.Builder()
                .scheme(returnUrlScheme)
                .authority(POPUP_BRIDGE_URL_HOST)
                .path("/some/cancel/flow")
                .build()
            every { intent.data } returns appSwitchReturnUri

            initializeClient()

            setPrivateExpectingAppSwitchReturn(subject, true)

            subject.handleReturnToApp(intent)
            testScheduler.advanceUntilIdle()
            runnableSlot.captured.run()

            verify { analyticsClient.sendEvent(POPUP_BRIDGE_APP_SWITCH_RETURNED) }
            verify {
                webViewMock.evaluateJavascript(withArg { script ->
                    assertTrue(script.contains("notifyCanceled()"))
                }, null)
            }
        }

    @Test
    fun `when handleReturnToApp is called with app switch intent and expectingAppSwitchReturn path not onCancel runs notifyComplete JS and sends APP_SWITCH_RETURNED`() =
        runTest {
            val appSwitchReturnUri = Uri.Builder()
                .scheme(returnUrlScheme)
                .authority(POPUP_BRIDGE_URL_HOST)
                .path("/onApprove")
                .appendQueryParameter("token", "abc")
                .build()
            every { intent.data } returns appSwitchReturnUri

            initializeClient()

            setPrivateExpectingAppSwitchReturn(subject, true)

            subject.handleReturnToApp(intent)
            testScheduler.advanceUntilIdle()
            runnableSlot.captured.run()

            verify { analyticsClient.sendEvent(POPUP_BRIDGE_APP_SWITCH_RETURNED) }
            verify {
                webViewMock.evaluateJavascript(withArg { script ->
                    assertTrue(script.contains("notifyComplete()") && script.contains("onComplete"))
                }, null)
            }
        }

    @Test
    fun `when handleReturnToApp consumes app switch return, the activity intent data is cleared`() =
        runTest {
            val appSwitchReturnUri = Uri.Builder()
                .scheme(returnUrlScheme)
                .authority(POPUP_BRIDGE_URL_HOST)
                .path("/onApprove")
                .build()
            val clearedIntentSlot = slot<Intent>()

            every { intent.data } returns appSwitchReturnUri
            every { activityMock.intent } returns Intent(Intent.ACTION_VIEW, appSwitchReturnUri)
            every { activityMock.intent = capture(clearedIntentSlot) } returns Unit

            initializeClient()
            setPrivateExpectingAppSwitchReturn(subject, true)

            subject.handleReturnToApp(intent)
            testScheduler.advanceUntilIdle()

            assertEquals(null, clearedIntentSlot.captured.data)
        }

    @Test
    fun `when onLaunchApp is invoked with valid url and main handler runs startActivity is called and APP_LAUNCHED is sent`() {
        initializeClient()

        onLaunchAppSlot.captured.invoke("https://www.paypal.com/checkout")

        // Run the runnable posted to the main handler (launchAppOnMainThread)
        Shadows.shadowOf(Looper.getMainLooper()).runToEndOfTasks()

        verify { activityMock.startActivity(any()) }
        verify { analyticsClient.sendEvent(POPUP_BRIDGE_APP_LAUNCHED) }
    }

    @Test
    fun `when onLaunchApp is invoked with PayPal app switch url the intent is pinned to the PayPal app package`() {
        val launchedIntent = slot<Intent>()
        every { activityMock.startActivity(capture(launchedIntent)) } returns Unit

        initializeClient()

        onLaunchAppSlot.captured.invoke("https://www.paypal.com/app-switch-checkout?token=EC-123")
        Shadows.shadowOf(Looper.getMainLooper()).runToEndOfTasks()

        assertEquals("com.paypal.android.p2pmobile", launchedIntent.captured.`package`)
    }

    @Test
    fun `when onLaunchApp is invoked with stale popup bridge intent, activity intent data is cleared before launch`() {
        val staleReturnUri = Uri.Builder()
            .scheme(returnUrlScheme)
            .authority(POPUP_BRIDGE_URL_HOST)
            .path("/onApprove")
            .build()
        val clearedIntentSlot = slot<Intent>()

        every { activityMock.intent } returns Intent(Intent.ACTION_VIEW, staleReturnUri)
        every { activityMock.intent = capture(clearedIntentSlot) } returns Unit

        initializeClient()

        onLaunchAppSlot.captured.invoke("https://www.paypal.com/checkout")
        Shadows.shadowOf(Looper.getMainLooper()).runToEndOfTasks()

        assertEquals(null, clearedIntentSlot.captured.data)
        verify { activityMock.startActivity(any()) }
    }

    @Test
    fun `when onLaunchApp is invoked and startActivity throws ActivityNotFoundException APP_LAUNCH_FAILED is sent and openUrl is called`() =
        runTest {
            initializeClient()
            every { activityMock.startActivity(any()) } throws android.content.ActivityNotFoundException()

            onLaunchAppSlot.captured.invoke("https://www.paypal.com/checkout")
            Shadows.shadowOf(Looper.getMainLooper()).runToEndOfTasks()
            testScheduler.advanceUntilIdle()

            verify { analyticsClient.sendEvent(POPUP_BRIDGE_APP_LAUNCH_FAILED) }
            verify { browserSwitchClient.start(activityMock, any()) }
        }

    @Test
    fun `when onLaunchApp is invoked with blank url errorListener is called and no startActivity`() {
        val errorListener: PopupBridgeErrorListener = mockk(relaxed = true)
        initializeClient()
        subject.setErrorListener(errorListener)

        onLaunchAppSlot.captured.invoke("   ")
        Shadows.shadowOf(Looper.getMainLooper()).runToEndOfTasks()

        verify { errorListener.onError(any()) }
        verify(exactly = 0) { activityMock.startActivity(any()) }
    }

    @Test
    fun `when onLaunchApp is invoked with null url errorListener is called and no startActivity`() {
        val errorListener: PopupBridgeErrorListener = mockk(relaxed = true)
        initializeClient()
        subject.setErrorListener(errorListener)

        onLaunchAppSlot.captured.invoke(null)
        Shadows.shadowOf(Looper.getMainLooper()).runToEndOfTasks()

        verify { errorListener.onError(any()) }
        verify(exactly = 0) { activityMock.startActivity(any()) }
    }

    @Test
    fun `when handleReturnToApp is called with intent data null browser switch path is used`() =
        runTest {
            every { intent.data } returns null

            initializeClient()

            subject.handleReturnToApp(intent)
            testScheduler.advanceUntilIdle()

            coVerify(atLeast = 1) { pendingRequestRepository.getPendingRequest() }
        }

    @Test
    fun `when handleReturnToApp is called with intent data host not popupbridgev1 browser switch path is used`() =
        runTest {
            val otherHostUri = Uri.Builder()
                .scheme(returnUrlScheme)
                .authority("other-host")
                .path("/path")
                .build()
            every { intent.data } returns otherHostUri

            initializeClient()

            subject.handleReturnToApp(intent)
            testScheduler.advanceUntilIdle()

            coVerify(atLeast = 1) { pendingRequestRepository.getPendingRequest() }
        }

    @Test
    fun `when handleReturnToApp is called with app switch intent but expectingAppSwitchReturn false no JS is run`() =
        runTest {
            val appSwitchReturnUri = Uri.Builder()
                .scheme(returnUrlScheme)
                .authority(POPUP_BRIDGE_URL_HOST)
                .path("/approve")
                .build()
            every { intent.data } returns appSwitchReturnUri

            initializeClient()
            // Do not set expectingAppSwitchReturn; handleAppSwitchReturn returns early

            subject.handleReturnToApp(intent)
            testScheduler.advanceUntilIdle()

            verify(exactly = 0) { analyticsClient.sendEvent(POPUP_BRIDGE_APP_SWITCH_RETURNED) }
            verify(exactly = 0) { webViewMock.evaluateJavascript(any(), any()) }
        }

    private fun setPrivateExpectingAppSwitchReturn(client: PopupBridgeClient, value: Boolean) {
        val field = PopupBridgeClient::class.java.getDeclaredField("expectingAppSwitchReturn")
        field.isAccessible = true
        field.setBoolean(client, value)
    }

    // endregion

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
