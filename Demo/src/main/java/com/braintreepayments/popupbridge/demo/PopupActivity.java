package com.braintreepayments.popupbridge.demo;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.braintreepayments.api.PopupBridgeClient;
import com.braintreepayments.api.PopupBridgeWebViewClient;

public class PopupActivity extends AppCompatActivity {

    private static final String RETURN_URL_SCHEME = "com.braintreepayments.popupbridgeexample";

    private WebView webView;
    private PopupBridgeClient popupBridgeClient;
    private PopupBridgeWebViewClient popupBridgeWebViewClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_popup);
        webView = findViewById(R.id.web_view);

        WebViewClient webViewClient = demoWebViewClient();

        popupBridgeWebViewClient = new PopupBridgeWebViewClient(webViewClient);

        popupBridgeClient = new PopupBridgeClient(this, webView, RETURN_URL_SCHEME, popupBridgeWebViewClient);
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

    private WebViewClient demoWebViewClient() {
        return new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Toast.makeText(PopupActivity.this, "Page Finished", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                Toast.makeText(PopupActivity.this, "Page Started", Toast.LENGTH_SHORT).show();
            }
        };
    }
}
