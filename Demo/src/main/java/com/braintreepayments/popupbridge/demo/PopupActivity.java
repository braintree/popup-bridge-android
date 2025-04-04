package com.braintreepayments.popupbridge.demo;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebView;

import androidx.appcompat.app.AppCompatActivity;

import com.braintreepayments.api.PopupBridgeClient;

public class PopupActivity extends AppCompatActivity {

    private static final String RETURN_URL_SCHEME = "com.braintreepayments.popupbridgeexample";

    private WebView webView;
    private PopupBridgeClient popupBridgeClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_popup);
        webView = findViewById(R.id.web_view);

        popupBridgeClient = new PopupBridgeClient(this, webView, RETURN_URL_SCHEME);
        popupBridgeClient.setErrorListener(error -> showDialog(error.getMessage()));

        webView.loadUrl(getIntent().getStringExtra("url"));
    }

    @Override
    protected void onResume() {
        super.onResume();
        popupBridgeClient.handleReturnToApp(getIntent());
    }

    @Override
    protected void onNewIntent(Intent newIntent) {
        super.onNewIntent(newIntent);
        popupBridgeClient.handleReturnToApp(newIntent);
    }

    public void showDialog(String message) {
        new AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss())
            .show();
    }
}
