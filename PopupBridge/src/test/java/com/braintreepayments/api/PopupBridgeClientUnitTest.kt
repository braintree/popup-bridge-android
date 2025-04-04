package com.braintreepayments.api

import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.json.JSONException
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.lang.ref.WeakReference

@RunWith(RobolectricTestRunner::class)
class PopupBridgeClientUnitTest {

    private val fragmentActivityMock: FragmentActivity = mockk(relaxed = true)
    private val webViewMock: WebView = mockk(relaxed = true)
    private val browserSwitchClient: BrowserSwitchClient = mockk(relaxed = true)
    private val pendingRequestRepository: PendingRequestRepository = mockk(relaxed = true)

    private lateinit var subject: PopupBridgeClient

    private val returnUrlScheme = "com.braintreepayments.popupbridgeexample"
    private val pendingRequest = "stored-pending-request"

    private val intent: Intent = mockk(relaxed = true)
    val runnableSlot = slot<Runnable>()

    private fun initializeClient(
        activity: FragmentActivity? = fragmentActivityMock,
        webView: WebView? = webViewMock
    ) {
        every { webViewMock.post(capture(runnableSlot)) } returns true
        every { pendingRequestRepository.getPendingRequest() } returns pendingRequest

        subject = PopupBridgeClient(
            activityRef = WeakReference(activity),
            webViewRef = WeakReference(webView),
            returnUrlScheme = returnUrlScheme,
            browserSwitchClient = browserSwitchClient,
            pendingRequestRepository = pendingRequestRepository
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `on init when activity is null, IllegalArgumentException is thrown`() {
        initializeClient(activity = null)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `on init when webView is null, IllegalArgumentException is thrown`() {
        initializeClient(webView = null)
    }

    @Test
    fun `on init, webView is set up`() {
        initializeClient()

        verify { webViewMock.settings.javaScriptEnabled = true }
        verify { webViewMock.addJavascriptInterface(subject, "popupBridge") }
    }

    @Test
    fun `when handleReturnToApp is called with a null pending request, nothing happens`() {
        initializeClient()
        every { pendingRequestRepository.getPendingRequest() } returns null

        subject.handleReturnToApp(intent)

        verify(exactly = 0) { browserSwitchClient.completeRequest(any(), any()) }
    }

    @Test
    fun `when handleReturnToApp is called, the request in pendingRequestRepository is cleared`() {
        initializeClient()

        subject.handleReturnToApp(intent)

        verify { pendingRequestRepository.clearPendingRequest() }
    }

    @Test
    fun `when handleReturnToApp is called and browser switch succeeds but uri host doesn't match, nothing happens`() {
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

        verify(exactly = 0) { webViewMock.post(any()) }
    }

    @Test
    fun `when handleReturnToApp is called and browser switch succeeds, success javascript is run`() {
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

        val payLoadJson = JSONObject().apply {
            val queryItems = JSONObject().apply {
                put("first", "first_value")
                put("second", "second_value")
            }
            put("path", "/mypath")
            put("queryItems", queryItems)
            put("hash", returnUrl.fragment)
        }
        val expectedJavascriptString = getExpectedSuccessJavascript(null, payLoadJson)

        initializeClient()

        subject.handleReturnToApp(intent)
        runnableSlot.captured.run()

        verify {
            webViewMock.evaluateJavascript(withArg { javascriptString ->
                assertEquals(expectedJavascriptString, javascriptString)
            }, null)
        }
    }

    @Test
    fun `when handleReturnToApp is called and browser switch succeeds but throws a JSONException, error is populated`() {
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
    fun `when handleReturnToApp is called and browser switch fails, canceled javascript is run`() {
        val browserSwitchFinalResult = mockk<BrowserSwitchFinalResult.Failure>()
        every { browserSwitchClient.completeRequest(intent, pendingRequest) } returns browserSwitchFinalResult

        initializeClient()

        subject.handleReturnToApp(intent)
        runnableSlot.captured.run()

        verify {
            webViewMock.evaluateJavascript(
                withArg { javascriptString ->
                    assertEquals(CANCELED_JAVASCRIPT, javascriptString)
                }, null
            )
        }
    }

    @Test
    fun `when handleReturnToApp is called and browser switch returns no result, canceled javascript is run`() {
        val browserSwitchFinalResult = mockk<BrowserSwitchFinalResult.NoResult>()
        every { browserSwitchClient.completeRequest(intent, pendingRequest) } returns browserSwitchFinalResult

        initializeClient()

        subject.handleReturnToApp(intent)
        runnableSlot.captured.run()

        verify {
            webViewMock.evaluateJavascript(withArg { javascriptString ->
                assertEquals(CANCELED_JAVASCRIPT, javascriptString)
            }, null)
        }
    }

    @Test
    fun `returnUrlPrefix returns expected url prefix`() {
        initializeClient()
        assertEquals(subject.returnUrlPrefix, "com.braintreepayments.popupbridgeexample://popupbridgev1/")
    }

    @Test
    fun `when open is called, the correct BrowserSwitchOptions are created`() {
        initializeClient()

        subject.open("https://example.com")

        verify(exactly = 1) {
            browserSwitchClient.start(fragmentActivityMock, withArg { browserSwitchOptions ->
                assertEquals("https://example.com".toUri(), browserSwitchOptions.url)
                assertEquals(1, browserSwitchOptions.requestCode)
                assertEquals(returnUrlScheme, browserSwitchOptions.returnUrlScheme)
            })
        }
    }

    @Test
    fun `when open is called with BrowserSwitchStartResult Started, the pendingRequest is stored in pendingRequestRepository`() {
        every { browserSwitchClient.start(fragmentActivityMock, any()) } returns
                BrowserSwitchStartResult.Started(pendingRequest)
        initializeClient()

        subject.open("https://example.com")

        verify { pendingRequestRepository.storePendingRequest(pendingRequest) }
    }

    @Test
    fun `when open is called with BrowserSwitchStartResult Failure, the error listener is invoked`() {
        val exception = Exception()
        every { browserSwitchClient.start(fragmentActivityMock, any()) } returns
                BrowserSwitchStartResult.Failure(exception)
        val errorListener: PopupBridgeErrorListener = mockk(relaxed = true)
        initializeClient()

        subject.setErrorListener(errorListener)
        subject.open("https://example.com")

        verify { errorListener.onError(exception) }
    }

    @Test
    fun `when open is called, the navigation listener is invoked`() {
        val url = "https://example.com"
        val navigationListener: PopupBridgeNavigationListener = mockk(relaxed = true)
        initializeClient()

        subject.setNavigationListener(navigationListener)
        subject.open(url)

        verify { navigationListener.onUrlOpened(url) }
    }

    @Test
    fun `when sendMessage is called, the message listener is invoked`() {
        val messageName = "messageName"
        val data = "data"
        val messageListener: PopupBridgeMessageListener = mockk(relaxed = true)
        initializeClient()

        subject.setMessageListener(messageListener)
        subject.sendMessage(messageName, data)

        verify { messageListener.onMessageReceived(messageName, data) }
    }

    @Test
    fun `when sendMessage without data is called, the message listener is invoked`() {
        val messageName = "messageName"
        val messageListener: PopupBridgeMessageListener = mockk(relaxed = true)
        initializeClient()

        subject.setMessageListener(messageListener)
        subject.sendMessage(messageName)

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