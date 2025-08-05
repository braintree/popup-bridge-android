package com.braintreepayments.popupbridge.demo;

import android.content.Context;
import android.graphics.Bitmap;
import android.webkit.WebView;
import android.widget.Toast;

import com.braintreepayments.api.PopupBridgeWebViewClient;

import java.lang.ref.WeakReference;

public class CustomPopupBridgeWebViewClient extends PopupBridgeWebViewClient {

    private final WeakReference<Context> contextWeakReference;

    CustomPopupBridgeWebViewClient(Context context) {
        super();
        contextWeakReference = new WeakReference<>(context);
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        // Additional actions can be added here if needed
        Toast.makeText(contextWeakReference.get(), "Page Finished", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
        Toast.makeText(contextWeakReference.get(), "Page Started", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        super.onReceivedError(view, errorCode, description, failingUrl);
        // Handle errors if needed
        Toast.makeText(contextWeakReference.get(), "Something went wrong", Toast.LENGTH_SHORT).show();
    }
}
