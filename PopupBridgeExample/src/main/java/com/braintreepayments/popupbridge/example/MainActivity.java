package com.braintreepayments.popupbridge.example;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.webkit.WebView;

import com.braintreepayments.popupbridge.PopupBridge;

public class MainActivity extends Activity {

    public static final String EXTRA_URL = "url";

    private static final String URL = "https://braintree.github.io/popup-bridge-example/";

    private WebView mWebView;
    private PopupBridge mPopupBridge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mWebView = findViewById(R.id.web_view);

        mPopupBridge = PopupBridge.newInstance(this, mWebView);

        String url = getIntent().getStringExtra(EXTRA_URL);

        if (TextUtils.isEmpty(url)) {
            url = URL;
        }

        mWebView.loadUrl(url);
    }
}
