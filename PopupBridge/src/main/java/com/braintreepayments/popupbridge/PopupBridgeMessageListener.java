package com.braintreepayments.popupbridge;

public interface PopupBridgeMessageListener {
    void onMessageReceived(String messageName, String data);
}
