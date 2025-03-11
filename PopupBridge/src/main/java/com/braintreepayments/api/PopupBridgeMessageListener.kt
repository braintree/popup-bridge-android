package com.braintreepayments.api

fun interface PopupBridgeMessageListener {
    fun onMessageReceived(messageName: String?, data: String?)
}
