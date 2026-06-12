package com.braintreepayments.api.internal

import android.net.Uri
import android.os.Looper
import androidx.activity.ComponentActivity
import io.mockk.mockk
import java.lang.ref.WeakReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class AppSwitchHandlerTest {

    private lateinit var subject: AppSwitchHandler

    @Before
    fun setUp() {
        subject = AppSwitchHandler(
            activityRef = WeakReference(mockk<ComponentActivity>(relaxed = true)),
            analyticsClient = mockk(relaxed = true),
            onOpenUrl = {},
            onError = {},
            onCanceled = {},
            onComplete = {},
        )
    }

    @Test
    fun `isVenmoAppSwitchUri returns true for Venmo checkout URL with query params`() {
        val uri = Uri.parse(
            "https://account.venmo.com/braintree/checkout" +
                "?resource_id=abc&x-success=myapp://success&x-cancel=myapp://cancel"
        )
        assertTrue(uri.isVenmoAppSwitchUri())
    }

    @Test
    fun `isVenmoAppSwitchUri returns false when path does not start with braintree checkout`() {
        val uri = Uri.parse("https://account.venmo.com/")
        assertFalse(uri.isVenmoAppSwitchUri())
    }

    @Test
    fun `isVenmoAppSwitchUri returns false for wrong host`() {
        val uri = Uri.parse("https://venmo.com/braintree/checkout")
        assertFalse(uri.isVenmoAppSwitchUri())
    }

    @Test
    fun `isVenmoAppSwitchUri returns false for non-https scheme`() {
        val uri = Uri.parse("http://account.venmo.com/braintree/checkout")
        assertFalse(uri.isVenmoAppSwitchUri())
    }

    @Test
    fun `rewriteToVenmoHost replaces account venmo com with venmo com`() {
        val uri = Uri.parse(
            "https://account.venmo.com/braintree/checkout" +
                "?resource_id=abc&x-success=myapp://success&x-cancel=myapp://cancel"
        )
        val rewritten = uri.rewriteToVenmoHost()
        assertEquals("venmo.com", rewritten.host)
        assertEquals("/braintree/checkout", rewritten.path)
        assertEquals("abc", rewritten.getQueryParameter("resource_id"))
        assertEquals("myapp://success", rewritten.getQueryParameter("x-success"))
    }

    // region path-segment matching tests (item #4)

    private val popupBridgeHost = PopupBridgeJavascriptInterface.POPUP_BRIDGE_URL_HOST

    private fun returnUri(path: String) = Uri.parse("popupbridgev1://$popupBridgeHost$path")

    private fun makeSubject(
        onCanceled: () -> Unit = {},
        onComplete: (Uri) -> Unit = {},
    ) = AppSwitchHandler(
        activityRef = WeakReference(mockk<ComponentActivity>(relaxed = true)),
        analyticsClient = mockk(relaxed = true),
        onOpenUrl = {},
        onError = { throw it },
        onCanceled = onCanceled,
        onComplete = onComplete,
    )

    private fun AppSwitchHandler.launchAndIdle(url: String = "https://www.paypal.com/app-switch-checkout") {
        launchApp(url)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun `shouldHandleReturn is false for cancellation path — partial segment must not match`() {
        val s = makeSubject()
        s.launchAndIdle()
        assertFalse(s.shouldHandleReturn(returnUri("/cancellation")))
    }

    @Test
    fun `shouldHandleReturn is false for approval path — partial segment must not match`() {
        val s = makeSubject()
        s.launchAndIdle()
        assertFalse(s.shouldHandleReturn(returnUri("/approval")))
    }

    @Test
    fun `handleReturn routes exact cancel segment to onCanceled`() {
        var canceled = false
        val s = makeSubject(onCanceled = { canceled = true })
        s.launchAndIdle()
        s.handleReturn(returnUri("/cancel"))
        assertTrue(canceled)
    }

    @Test
    fun `handleReturn routes oncancel segment to onCanceled`() {
        var canceled = false
        val s = makeSubject(onCanceled = { canceled = true })
        s.launchAndIdle()
        s.handleReturn(returnUri("/oncancel"))
        assertTrue(canceled)
    }

    @Test
    fun `handleReturn with cancellation path routes to onComplete not onCanceled`() {
        var completed = false
        var canceled = false
        val s = makeSubject(onCanceled = { canceled = true }, onComplete = { completed = true })
        s.launchAndIdle()
        s.handleReturn(returnUri("/cancellation"))
        assertTrue(completed)
        assertFalse(canceled)
    }

    @Test
    fun `handleReturn routes approve segment to onComplete`() {
        var completed = false
        val s = makeSubject(onComplete = { completed = true })
        s.launchAndIdle()
        s.handleReturn(returnUri("/approve"))
        assertTrue(completed)
    }

    @Test
    fun `handleReturn routes error segment to onComplete`() {
        var completed = false
        val s = makeSubject(onComplete = { completed = true })
        s.launchAndIdle()
        s.handleReturn(returnUri("/error"))
        assertTrue(completed)
    }

    // endregion
}
