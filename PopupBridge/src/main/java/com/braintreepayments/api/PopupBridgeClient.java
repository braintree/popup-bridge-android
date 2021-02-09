package com.braintreepayments.api;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.Set;

public class PopupBridgeClient {

    public static final String POPUP_BRIDGE_NAME = "popupBridge";
    public static final String POPUP_BRIDGE_URL_HOST = "popupbridgev1";

    private WeakReference<FragmentActivity> activityRef;
    private WeakReference<WebView> webViewRef;
    private PopupBridgeNavigationListener navigationListener;
    private PopupBridgeMessageListener messageListener;
    private String returnUrlScheme;

    private BrowserSwitchClient browserSwitchClient;

    /**
     * Create a new instance of {@link PopupBridgeClient}.
     *
     * This will enable JavaScript in your WebView.
     *
     * @param activity The {@link FragmentActivity} to add the {@link Fragment} to.
     * @param webView The {@link WebView} to enable for PopupBridge.
     * @return {@link PopupBridgeClient}
     * @throws IllegalArgumentException If the activity is not valid or the fragment cannot be added.
     */
    @SuppressLint("SetJavaScriptEnabled")
    public PopupBridgeClient(FragmentActivity activity, WebView webView) throws IllegalArgumentException {
        if (activity == null) {
            throw new IllegalArgumentException("Activity is null");
        }

        if (webView == null) {
            throw new IllegalArgumentException("WebView is null");
        }

        webView.getSettings().setJavaScriptEnabled(true);
        webView.addJavascriptInterface(this, POPUP_BRIDGE_NAME);
        webViewRef = new WeakReference<>(webView);

        activityRef = new WeakReference<>(activity);
        returnUrlScheme = activity.getPackageName().toLowerCase().replace("_", "") + ".popupbridge";
        browserSwitchClient = new BrowserSwitchClient();
    }

    public void handlePopupBridgeResult(FragmentActivity activity) {
        BrowserSwitchResult result = browserSwitchClient.deliverResult(activity);
        if (result != null) {
            onBrowserSwitchResult(result);
        }
    }

    private void runJavaScriptInWebView(final String script) {
        WebView webView = webViewRef.get();
        if (webView == null) {
            return;
        }
        webView.post(new Runnable() {
            @Override
            public void run() {
                WebView webViewInternal = webViewRef.get();
                if (webViewInternal == null) {
                    return;
                }
                webViewInternal.evaluateJavascript(script, null);
            }
        });
    }

    public void onBrowserSwitchResult(BrowserSwitchResult result) {
        String error = null;
        String payload = null;

        Uri returnUri = result.getDeepLinkUrl();
        if (result.getStatus() == BrowserSwitchStatus.CANCELED) {
            runJavaScriptInWebView(""
                + "if (typeof window.popupBridge.onCancel === 'function') {"
                + "  window.popupBridge.onCancel();"
                + "} else {"
                + "  window.popupBridge.onComplete(null, null);"
                + "}");
            return;
        } else if (result.getStatus() == BrowserSwitchStatus.SUCCESS) {
            if (returnUri == null || !returnUri.getScheme().equals(getReturnUrlScheme()) ||
                    !returnUri.getHost().equals(POPUP_BRIDGE_URL_HOST)) {
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

        runJavaScriptInWebView(String.format("window.popupBridge.onComplete(%s, %s);", error, payload));
    }

    public String getReturnUrlScheme() {
        return returnUrlScheme;
    }

    @JavascriptInterface
    public String getReturnUrlPrefix() {
        return String.format("%s://%s/", getReturnUrlScheme(), POPUP_BRIDGE_URL_HOST);
    }

    @JavascriptInterface
    public void open(String url) {
        FragmentActivity activity = activityRef.get();
        if (activity == null) {
            return;
        }
        BrowserSwitchOptions browserSwitchOptions = new BrowserSwitchOptions()
                .requestCode(1)
                .url(Uri.parse(url))
                .returnUrlScheme(getReturnUrlScheme());
        try {
            browserSwitchClient.start(activity, browserSwitchOptions);
        } catch (Exception e) {
            // TODO: handle errors thrown from browser switch and unit test
        }

        if (navigationListener != null) {
            navigationListener.onUrlOpened(url);
        }
    }

    @JavascriptInterface
    public void sendMessage(String messageName) {
        if (messageListener != null) {
            messageListener.onMessageReceived(messageName, null);
        }
    }

    @JavascriptInterface
    public void sendMessage(String messageName, String data) {
        if (messageListener != null) {
            messageListener.onMessageReceived(messageName, data);
        }
    }

    public void setNavigationListener(PopupBridgeNavigationListener listener) {
        navigationListener = listener;
    }

    public void setMessageListener(PopupBridgeMessageListener listener) {
        messageListener = listener;
    }
}
