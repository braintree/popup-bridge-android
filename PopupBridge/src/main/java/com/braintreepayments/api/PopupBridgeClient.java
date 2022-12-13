package com.braintreepayments.api;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.Set;

public class PopupBridgeClient {

    // TODO: consider using a `com.braintreepayments...` prefixed request key
    // to prevent shared preferences collisions with other braintree libs that use browser switch
    static final int REQUEST_CODE = 1;

    static final String POPUP_BRIDGE_NAME = "popupBridge";
    static final String POPUP_BRIDGE_URL_HOST = "popupbridgev1";

    private final WeakReference<FragmentActivity> activityRef;
    private final WeakReference<WebView> webViewRef;
    private PopupBridgeNavigationListener navigationListener;
    private PopupBridgeMessageListener messageListener;
    private PopupBridgeErrorListener errorListener;
    private final String returnUrlScheme;

    private final BrowserSwitchClient browserSwitchClient;

    /**
     * Create a new instance of {@link PopupBridgeClient}.
     *
     * This will enable JavaScript in your WebView.
     *
     * @param activity The {@link FragmentActivity} to add the {@link Fragment} to.
     * @param webView The {@link WebView} to enable for PopupBridge.
     * @param returnUrlScheme The return url scheme to use for deep linking back into the application.
     * @throws IllegalArgumentException If the activity is not valid or the fragment cannot be added.
     */
    public PopupBridgeClient(FragmentActivity activity, WebView webView, String returnUrlScheme) throws IllegalArgumentException {
        this(new WeakReference<>(activity), new WeakReference<>(webView), returnUrlScheme, new BrowserSwitchClient());
    }

    @SuppressLint("SetJavaScriptEnabled")
    @VisibleForTesting
    PopupBridgeClient(WeakReference<FragmentActivity> activityRef, WeakReference<WebView> webViewRef, String returnUrlScheme, BrowserSwitchClient browserSwitchClient) throws IllegalArgumentException {
        FragmentActivity activity = activityRef.get();
        if (activity == null) {
            throw new IllegalArgumentException("Activity is null");
        }

        WebView webView = webViewRef.get();
        if (webView == null) {
            throw new IllegalArgumentException("WebView is null");
        }

        webView.getSettings().setJavaScriptEnabled(true);
        webView.addJavascriptInterface(this, POPUP_BRIDGE_NAME);
        this.webViewRef = webViewRef;
        this.activityRef = activityRef;

        this.returnUrlScheme = returnUrlScheme;
        this.browserSwitchClient = browserSwitchClient;

        PopupBridgeLifecycleObserver observer =
            new PopupBridgeLifecycleObserver(this);
        activity.getLifecycle().addObserver(observer);
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

    BrowserSwitchResult getBrowserSwitchResult(FragmentActivity activity) {
        return browserSwitchClient.getResult(activity);
    }

    BrowserSwitchResult deliverBrowserSwitchResult(FragmentActivity activity) {
        return browserSwitchClient.deliverResult(activity);
    }

    void onBrowserSwitchResult(BrowserSwitchResult result) {
        String error = null;
        String payload = null;

        Uri returnUri = result.getDeepLinkUrl();
        if (result.getStatus() == BrowserSwitchStatus.CANCELED) {
            runJavaScriptInWebView(""
                + "function notifyCanceled() {"
                + "  if (typeof window.popupBridge.onCancel === 'function') {"
                + "    window.popupBridge.onCancel();"
                + "  } else {"
                + "    window.popupBridge.onComplete(null, null);"
                + "  }"
                + "}"
                + ""
                + "if (document.readyState === 'complete') {"
                + "  notifyCanceled();"
                + "} else {"
                + "  window.addEventListener('load', function () {"
                + "    notifyCanceled();"
                + "  });"
                + "}");
            return;
        } else if (result.getStatus() == BrowserSwitchStatus.SUCCESS) {
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

    @JavascriptInterface
    public String getReturnUrlPrefix() {
        return String.format("%s://%s/", returnUrlScheme, POPUP_BRIDGE_URL_HOST);
    }

    @JavascriptInterface
    public void open(String url) {
        FragmentActivity activity = activityRef.get();
        if (activity == null) {
            return;
        }
        BrowserSwitchOptions browserSwitchOptions = new BrowserSwitchOptions()
                .requestCode(REQUEST_CODE)
                .url(Uri.parse(url))
                .returnUrlScheme(returnUrlScheme);
        try {
            browserSwitchClient.start(activity, browserSwitchOptions);
        } catch (Exception e) {
            if (errorListener != null) {
                errorListener.onError(e);
            }
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

    public void setErrorListener(PopupBridgeErrorListener listener) {
        errorListener = listener;
    }
}
