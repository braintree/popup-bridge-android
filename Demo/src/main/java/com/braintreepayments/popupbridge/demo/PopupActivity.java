package com.braintreepayments.popupbridge.demo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.webkit.WebView;

import androidx.appcompat.app.AppCompatActivity;

import com.braintreepayments.api.PopupBridgeClient;

public class PopupActivity extends AppCompatActivity {

    static final String BUNDLE_KEY_URL = "PopupActivity.BUNDLE_KEY_URL";

    private WebView mWebView;
    private PopupBridgeClient mPopupBridgeClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_popup);
        mWebView = findViewById(R.id.web_view);

        mPopupBridgeClient = new PopupBridgeClient(this, mWebView, "com.braintreepayments.popupbridgeexample");
        mPopupBridgeClient.setErrorListener(error -> showDialog(error.getMessage()));

        String url = getIntent().getStringExtra(BUNDLE_KEY_URL);
        if (url == null) {
            // assume launch is from deep link; fetch url from persistent storage
            url = getPendingURLFromPersistentStorage();
        }

        mWebView.loadUrl(url);
        savePendingURL(url);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mPopupBridgeClient.deliverPopupBridgeResult(this);
    }

    @Override
    protected void onNewIntent(Intent newIntent) {
        super.onNewIntent(newIntent);
        setIntent(newIntent);
    }

    public void showDialog(String message) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss())
                .show();
    }

    private String getPendingURLFromPersistentStorage() {
        SharedPreferences sharedPreferences = getPreferences(Context.MODE_PRIVATE);
        if (sharedPreferences != null) {
            return sharedPreferences.getString(BUNDLE_KEY_URL, null);
        }
        return null;
    }

    private void savePendingURL(String url) {
        SharedPreferences sharedPreferences = getPreferences(Context.MODE_PRIVATE);
        if (sharedPreferences != null) {
            sharedPreferences.edit()
                    .putString(BUNDLE_KEY_URL, url)
                    .apply();
        }
    }
}
