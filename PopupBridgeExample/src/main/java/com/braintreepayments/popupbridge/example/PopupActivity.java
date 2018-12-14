package com.braintreepayments.popupbridge.example;

import android.os.Bundle;
import android.webkit.WebView;

import com.braintreepayments.popupbridge.PopupBridge;

import androidx.appcompat.app.AppCompatActivity;

public class PopupActivity extends AppCompatActivity {

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
