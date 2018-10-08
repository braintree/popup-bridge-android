package com.braintreepayments.popupbridge.example;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

import com.braintreepayments.popupbridge.PopupBridge;

public class PopupActivity extends Activity {

    private WebView mWebView;
    private PopupBridge mPopupBridge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_popup);
        mWebView = findViewById(R.id.web_view);

        mPopupBridge = PopupBridge.newInstance(this, mWebView);

        mWebView.loadUrl(getIntent().getData().toString());
    }
}
