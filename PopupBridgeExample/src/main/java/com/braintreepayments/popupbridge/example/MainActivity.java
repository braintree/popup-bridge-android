package com.braintreepayments.popupbridge.example;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

import com.braintreepayments.popupbridge.PopupBridge;

public class MainActivity extends Activity {

    private static final String URL = "https://braintree.github.io/popup-bridge-example/";

    private WebView mWebView;
    private PopupBridge mPopupBridge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mWebView = (WebView) findViewById(R.id.web_view);

        mPopupBridge = PopupBridge.newInstance(this, mWebView);

        mWebView.loadUrl(URL);
    }
}
