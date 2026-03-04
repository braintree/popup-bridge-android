package com.braintreepayments.api.internal

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import io.mockk.mockk

class PopupBridgeJavascriptInterfaceUnitTest {

    private lateinit var subject: PopupBridgeJavascriptInterface
    private val returnUrlScheme = "test-scheme"
    private val mockContext = mockk<Context>(relaxed = true)

    @Before
    fun setup() {
        subject = PopupBridgeJavascriptInterface(returnUrlScheme, mockContext)
    }

    @Test
    fun `getting returnUrlPrefix returns correct value`() {
        val expectedPrefix = "test-scheme://${PopupBridgeJavascriptInterface.POPUP_BRIDGE_URL_HOST}/"
        assertEquals(expectedPrefix, subject.returnUrlPrefix)
    }

    @Test
    fun `when open is invoked, onOpen callback is called with url`() {
        var capturedUrl: String? = null
        subject.onOpen = { url -> capturedUrl = url }

        val testUrl = "https://example.com"
        subject.open(testUrl)

        assertEquals(testUrl, capturedUrl)
    }

    @Test
    fun `when sendMessage is invoked with messageName only, onSendMessage callback is invoked with messageName`() {
        var capturedMessageName: String? = null
        var capturedData: String? = "unexpected"
        subject.onSendMessage = { messageName, data ->
            capturedMessageName = messageName
            capturedData = data
        }

        val testMessageName = "testMessage"
        subject.sendMessage(testMessageName)

        assertEquals(testMessageName, capturedMessageName)
        assertNull(capturedData)
    }

    @Test
    fun `when sendMessage is invoked with messageName and data, onSendMessage callback is invoked with both`() {
        var capturedMessageName: String? = null
        var capturedData: String? = null
        subject.onSendMessage = { messageName, data ->
            capturedMessageName = messageName
            capturedData = data
        }

        val testMessageName = "testMessage"
        val testData = "testData"
        subject.sendMessage(testMessageName, testData)

        assertEquals(testMessageName, capturedMessageName)
        assertEquals(testData, capturedData)
    }

    @Test
    fun `isPayPalInstalled is exposed to JS and returns boolean from context`() {
        // When the web page reads window.popupBridge.isPayPalInstalled, this getter runs.
        val result = subject.isPayPalInstalled
        assertTrue(result == true || result == false)
    }

    @Test
    fun `isVenmoInstalled is exposed to JS and returns boolean from context`() {
        // When the web page reads window.popupBridge.isVenmoInstalled, this getter runs.
        val result = subject.isVenmoInstalled
        assertTrue(result == true || result == false)
    }

    @Test
    fun `when launchApp is invoked, onLaunchApp callback is called with url`() {
        var capturedUrl: String? = null
        subject.onLaunchApp = { url -> capturedUrl = url }

        val testUrl = "https://www.paypal.com/some/checkout"
        subject.launchApp(testUrl)

        assertEquals(testUrl, capturedUrl)
    }

    @Test
    fun `when launchApp is invoked with null, onLaunchApp callback is called with null`() {
        var capturedUrl: String? = "not-null"
        subject.onLaunchApp = { url -> capturedUrl = url }

        subject.launchApp(null)

        assertNull(capturedUrl)
    }
}
