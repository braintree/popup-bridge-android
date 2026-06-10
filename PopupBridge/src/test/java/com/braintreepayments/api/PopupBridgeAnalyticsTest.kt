package com.braintreepayments.api

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for [PopupBridgeAnalytics] constants (Popup Bridge++ app-switch events).
 */
class PopupBridgeAnalyticsTest {

    @Test
    fun `app switch analytics constants have expected values`() {
        assertEquals("popup-bridge:app-switch:available", PopupBridgeAnalytics.POPUP_BRIDGE_APP_DETECTED)
        assertEquals("popup-bridge:app-switch:started", PopupBridgeAnalytics.POPUP_BRIDGE_APP_LAUNCHED)
        assertEquals("popup-bridge:app-switch:failed", PopupBridgeAnalytics.POPUP_BRIDGE_APP_LAUNCH_FAILED)
        assertEquals("popup-bridge:app-switch:succeeded", PopupBridgeAnalytics.POPUP_BRIDGE_APP_SWITCH_RETURNED)
    }

    @Test
    fun `existing analytics constants unchanged`() {
        assertEquals("popup-bridge:started", PopupBridgeAnalytics.POPUP_BRIDGE_STARTED)
        assertEquals("popup-bridge:succeeded", PopupBridgeAnalytics.POPUP_BRIDGE_SUCCEEDED)
        assertEquals("popup-bridge:failed", PopupBridgeAnalytics.POPUP_BRIDGE_FAILED)
        assertEquals("popup-bridge:canceled", PopupBridgeAnalytics.POPUP_BRIDGE_CANCELED)
    }
}
