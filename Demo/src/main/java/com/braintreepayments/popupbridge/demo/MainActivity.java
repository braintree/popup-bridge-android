package com.braintreepayments.popupbridge.demo;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final String POPUP_BRIDGE_URL = "https://braintree.github.io/popup-bridge-example/";
    private static final String PAYPAL_POPUP_BRIDGE_URL = "https://braintree.github.io/popup-bridge-example/paypal";
    private static final String PAYPAL_CHECKOUTJS_POPUP_BRIDGE_URL = "https://braintree.github.io/popup-bridge-example/paypal-checkout.html";
    private static final String VENMO_POPUP_BRIDGE_URL = "https://braintree.github.io/popup-bridge-example/venmo";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    private void switchToWebView(String url) {
        Intent intent = new Intent(this, PopupActivity.class);
        intent.putExtra("url", url);
        startActivity(intent);
    }

    public void onPopupBridgeClick(View view) {
        switchToWebView(POPUP_BRIDGE_URL);
    }

    public void onPayPalPopupBridgeClick(View view) {
        switchToWebView(PAYPAL_POPUP_BRIDGE_URL);
    }

    public void onPayPalCheckoutJSPopupBridgeClick(View view) {
        switchToWebView(PAYPAL_CHECKOUTJS_POPUP_BRIDGE_URL);
    }

    public void onVenmoPopupBridgeClick(View view) {
        switchToWebView(VENMO_POPUP_BRIDGE_URL);
    }
}
