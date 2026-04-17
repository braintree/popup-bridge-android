package com.braintreepayments.api

internal object PopupBridgeAnalytics {
    const val POPUP_BRIDGE_STARTED = "popup-bridge:started"
    const val POPUP_BRIDGE_SUCCEEDED = "popup-bridge:succeeded"
    const val POPUP_BRIDGE_FAILED = "popup-bridge:failed"
    const val POPUP_BRIDGE_CANCELED = "popup-bridge:canceled"
    const val POPUP_BRIDGE_APP_DETECTED = "popup-bridge:app-switch:app-detected"
    const val POPUP_BRIDGE_APP_LAUNCHED = "popup-bridge:app-switch:app-launched"
    const val POPUP_BRIDGE_APP_LAUNCH_FAILED = "popup-bridge:app-switch:app-launch-failed"
    const val POPUP_BRIDGE_APP_SWITCH_RETURNED = "popup-bridge:app-switch:returned"
}
