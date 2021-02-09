package com.braintreepayments.popupbridge.demo;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebView;

import com.braintreepayments.api.PopupBridge;

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

        mWebView.loadUrl(getIntent().getStringExtra("url"));
    }

    @Override
    protected void onNewIntent(Intent newIntent) {
        super.onNewIntent(newIntent);
        setIntent(newIntent);
    }
}
