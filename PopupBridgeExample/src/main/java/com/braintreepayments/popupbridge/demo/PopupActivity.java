package com.braintreepayments.popupbridge.demo;

import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebView;

import com.braintreepayments.api.PopupBridgeClient;

import androidx.appcompat.app.AppCompatActivity;

public class PopupActivity extends AppCompatActivity {

    private WebView mWebView;
    private PopupBridgeClient mPopupBridgeClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_popup);
        mWebView = findViewById(R.id.web_view);

        mPopupBridgeClient = new PopupBridgeClient(this, mWebView);

        mWebView.loadUrl(getIntent().getStringExtra("url"));
    }

    @Override
    protected void onResume() {
        super.onResume();
        mPopupBridgeClient.handlePopupBridgeResult(this);
    }

    @Override
    protected void onNewIntent(Intent newIntent) {
        super.onNewIntent(newIntent);
        setIntent(newIntent);
    }
}
