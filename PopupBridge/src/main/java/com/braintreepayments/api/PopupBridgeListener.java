package com.braintreepayments.api;

public interface PopupBridgeListener {
    void onPopupBridgeError(Exception error);
    void onPopupBridgeUrlOpened(String url);

    void onPopupBridgeMessageReceived(String messageName, String data);
}
