package com.braintreepayments.api;

import static com.braintreepayments.api.PopupBridgeClient.POPUP_BRIDGE_URL_HOST;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.fragment.app.FragmentActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Set;

public class PopupBridgeClient2 {

    private static final String POPUP_BRIDGE_JS_GLOBAL_NAME = "popupBridge";
    private static final int POPUP_BRIDGE_REQUEST_CODE = 1;

    private final FragmentActivity activity;
    private final String returnUrlScheme;
    private final BrowserSwitchClient browserSwitchClient;

    private WebView webView;
    private PopupBridgeListener listener;
    private String pendingBrowserSwitchId;

    public PopupBridgeClient2(FragmentActivity activity, String returnUrlScheme) {
        this.activity = activity;
        this.returnUrlScheme = returnUrlScheme;
        this.browserSwitchClient = new BrowserSwitchClient();
    }

    public void setListener(PopupBridgeListener listener) {
        this.listener = listener;
    }

    public void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            pendingBrowserSwitchId = savedInstanceState.getString("pendingBrowserSwitchId");
        }
    }

    public void onResume(FragmentActivity activity) {
        BrowserSwitchResult result = browserSwitchClient.deliverResult(activity);
        if (result != null) {
            onBrowserSwitchResult(result);
        }
    }

    public void onSaveInstanceState(Bundle outState) {
        if (outState != null) {
            outState.putString("pendingBrowserSwitchId", pendingBrowserSwitchId);
        }
    }

    public void onNewIntent(Context context, Intent newIntent) {
        BrowserSwitchResult result = browserSwitchClient.onNewIntent(context, newIntent);
        if (result != null) {
            onBrowserSwitchResult(result);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void bindToWebView(WebView webView) {
        this.webView = webView;
        if (webView != null) {
            WebSettings webViewSettings = webView.getSettings();
            if (webViewSettings != null) {
                webViewSettings.setJavaScriptEnabled(true);
            }
            webView.addJavascriptInterface(this, POPUP_BRIDGE_JS_GLOBAL_NAME);
        }
    }

    @JavascriptInterface
    public String getReturnUrlPrefix() {
        return String.format("%s://%s/", returnUrlScheme, POPUP_BRIDGE_URL_HOST);
    }

    @JavascriptInterface
    public void open(String url) {
        BrowserSwitchOptions browserSwitchOptions = new BrowserSwitchOptions()
                .requestCode(POPUP_BRIDGE_REQUEST_CODE)
                .url(Uri.parse(url))
                .returnUrlScheme(returnUrlScheme);

        try {
            pendingBrowserSwitchId = browserSwitchClient.start(activity, browserSwitchOptions);
        } catch (BrowserSwitchException e) {
            if (listener != null) {
                listener.onPopupBridgeError(e);
            }
        }

        if (listener != null) {
            listener.onPopupBridgeUrlOpened(url);
        }
    }

    @JavascriptInterface
    public void sendMessage(String messageName) {
        if (listener != null) {
            listener.onPopupBridgeMessageReceived(messageName, null);
        }
    }

    @JavascriptInterface
    public void sendMessage(String messageName, String data) {
        if (listener != null) {
            listener.onPopupBridgeMessageReceived(messageName, data);
        }
    }

    @JavascriptInterface
    public boolean isBrowserSwitchInProgress() {
        if (pendingBrowserSwitchId != null) {
            return browserSwitchClient.isBrowserSwitchInProgress(activity, pendingBrowserSwitchId);
        }
        return false;
    }

    private void onBrowserSwitchResult(BrowserSwitchResult result) {
        String error = null;
        String payload = null;

        Uri returnUri = result.getDeepLinkUrl();
        if (result.getStatus() == BrowserSwitchStatus.SUCCESS) {
            if (returnUri == null || !returnUri.getHost().equals(POPUP_BRIDGE_URL_HOST)) {
                return;
            }

            JSONObject json = new JSONObject();
            JSONObject queryItems = new JSONObject();

            Set<String> queryParams = returnUri.getQueryParameterNames();
            if (queryParams != null && !queryParams.isEmpty()) {
                for (String queryParam : queryParams) {
                    try {
                        queryItems.put(queryParam, returnUri.getQueryParameter(queryParam));
                    } catch (JSONException e) {
                        error = "new Error('Failed to parse query items from return URL. " +
                                e.getLocalizedMessage() + "')";
                    }
                }
            }

            try {
                json.put("path", returnUri.getPath());
                json.put("queryItems", queryItems);
                json.put("hash", returnUri.getFragment());
            } catch (JSONException ignored) {
            }

            payload = json.toString();
        }

        String successJavascript = String.format(""
                + "function notifyComplete() {"
                + "  window.popupBridge.onComplete(%s, %s);"
                + "}"
                + ""
                + "if (document.readyState === 'complete') {"
                + "  notifyComplete();"
                + "} else {"
                + "  window.addEventListener('load', function () {"
                + "    notifyComplete();"
                + "  });"
                + "}", error, payload);

        runJavaScriptInWebView(successJavascript);
    }

    private void runJavaScriptInWebView(final String script) {
        webView.post(new Runnable() {
            @Override
            public void run() {
                webView.evaluateJavascript(script, null);
            }
        });
    }
}
