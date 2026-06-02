package com.braintreepayments.api.internal

import android.net.Uri
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
}
