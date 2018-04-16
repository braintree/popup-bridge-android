package com.braintreepayments.popupbridge.example;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends Activity {

    private static final String POPUP_BRIDGE_URL = "https://braintree.github.io/popup-bridge-example/";
    private static final String PAYPAL_POPUP_BRIDGE_URL = "https://braintree.github.io/popup-bridge-example/paypal-checkout.html";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    private void switchToWebView(String url) {
        Intent intent = new Intent(this, PopupActivity.class);
        intent.setData(Uri.parse(url));
        startActivity(intent);
    }

    public void onPopupBridgeClick(View view) {
        switchToWebView(POPUP_BRIDGE_URL);
    }

    public void onPayPalPopupBridgeClick(View view) {
        switchToWebView(PAYPAL_POPUP_BRIDGE_URL);
    }
}
