package com.braintreepayments.popupbridge.demo;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.braintreepayments.api.PopupBridgeClient2;
import com.braintreepayments.api.PopupBridgeListener;

public class PopupActivity extends AppCompatActivity implements PopupBridgeListener {

    private static final String RETURN_URL_SCHEME = "com.braintreepayments.popupbridgeexample";

    private WebView mWebView;
    private PopupBridgeClient2 popupBridgeClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_popup);
        setTitle("Host Activity");

        mWebView = findViewById(R.id.web_view);

        popupBridgeClient = new PopupBridgeClient2(this, RETURN_URL_SCHEME);
        popupBridgeClient.setListener(this);
        popupBridgeClient.onCreate(savedInstanceState);
        popupBridgeClient.bindToWebView(mWebView);

        mWebView.loadUrl(getIntent().getStringExtra("url"));
    }

    @Override
    protected void onResume() {
        super.onResume();
        popupBridgeClient.onResume(this);
    }

    @Override
    protected void onNewIntent(Intent newIntent) {
        super.onNewIntent(newIntent);
        popupBridgeClient.onNewIntent(this, newIntent);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        popupBridgeClient.onSaveInstanceState(outState);
    }

    public void showDialog(String message) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    public void onPopupBridgeError(Exception error) {
        showDialog(error.getMessage());
    }

    @Override
    public void onPopupBridgeUrlOpened(String url) {

    }

    @Override
    public void onPopupBridgeMessageReceived(String messageName, String data) {

    }
}
