package com.braintreepayments.api;

public interface PopupBridgeMessageListener {
    void onMessageReceived(String messageName, String data);
}
