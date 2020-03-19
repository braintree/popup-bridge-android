package com.braintreepayments.popupbridge;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.braintreepayments.browserswitch.BrowserSwitchFragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Set;

public class PopupBridge extends BrowserSwitchFragment {

    public static final String POPUP_BRIDGE_NAME = "popupBridge";
    public static final String POPUP_BRIDGE_URL_HOST = "popupbridgev1";

    private static final String TAG = "com.braintreepayments.popupbridge";

    private WebView mWebView;
    private PopupBridgeNavigationListener mNavigationListener;
    private PopupBridgeMessageListener mMessageListener;

    public PopupBridge() {}

    /**
     * NEXT_MAJOR_VERSION: remove this method in favor of PopupBridge#newInstance(FragmentActivity, WebView)
     * Create a new instance of {@link PopupBridge} and add it to the {@link AppCompatActivity}'s {@link android.support.v4.app.FragmentManager}.
     *
     * This will enable JavaScript in your WebView.
     *
     * @param activity The {@link AppCompatActivity} to add the {@link Fragment} to.
     * @param webView The {@link WebView} to enable for PopupBridge.
     * @return {@link PopupBridge}
     * @throws IllegalArgumentException If the activity is not valid or the fragment cannot be added.
     */
    @SuppressLint("SetJavaScriptEnabled")
    public static PopupBridge newInstance(AppCompatActivity activity, WebView webView) throws IllegalArgumentException {
        return newInstance((FragmentActivity)activity, webView);
    }

    /**
     * Create a new instance of {@link PopupBridge} and add it to the {@link FragmentActivity}'s {@link android.support.v4.app.FragmentManager}.
     *
     * This will enable JavaScript in your WebView.
     *
     * @param activity The {@link FragmentActivity} to add the {@link Fragment} to.
     * @param webView The {@link WebView} to enable for PopupBridge.
     * @return {@link PopupBridge}
     * @throws IllegalArgumentException If the activity is not valid or the fragment cannot be added.
     */
    @SuppressLint("SetJavaScriptEnabled")
    public static PopupBridge newInstance(FragmentActivity activity, WebView webView) throws IllegalArgumentException {
        if (activity == null) {
            throw new IllegalArgumentException("Activity is null");
        }

        if (webView == null) {
            throw new IllegalArgumentException("WebView is null");
        }

        FragmentManager fm = activity.getSupportFragmentManager();
        PopupBridge popupBridge = (PopupBridge) fm.findFragmentByTag(TAG);
        if (popupBridge == null) {
            popupBridge = new PopupBridge();
            Bundle bundle = new Bundle();

            popupBridge.setArguments(bundle);

            try {
                if (VERSION.SDK_INT >= VERSION_CODES.N) {
                    try {
                        fm.beginTransaction().add(popupBridge, TAG).commitNow();
                    } catch (IllegalStateException | NullPointerException e) {
                        fm.beginTransaction().add(popupBridge, TAG).commit();
                        try {
                            fm.executePendingTransactions();
                        } catch (IllegalStateException ignored) {}
                    }
                } else {
                    fm.beginTransaction().add(popupBridge, TAG).commit();
                    try {
                        fm.executePendingTransactions();
                    } catch (IllegalStateException ignored) {}
                }
            } catch (IllegalStateException e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }

        webView.getSettings().setJavaScriptEnabled(true);

        popupBridge.mContext = activity.getApplicationContext();
        popupBridge.mWebView = webView;
        popupBridge.mWebView.addJavascriptInterface(popupBridge, POPUP_BRIDGE_NAME);

        return popupBridge;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    private void runJavaScriptInWebView(final String script) {
        mWebView.post(new Runnable() {
            @Override
            public void run() {
                mWebView.evaluateJavascript(script, null);
            }
        });
    }

    @Override
    public void onBrowserSwitchResult(int requestCode, BrowserSwitchResult result, Uri returnUri) {
        String error = null;
        String payload = null;

        if (result == BrowserSwitchResult.CANCELED) {
            runJavaScriptInWebView(""
                + "if (typeof window.popupBridge.onCancel === 'function') {"
                + "  window.popupBridge.onCancel();"
                + "} else {"
                + "  window.popupBridge.onComplete(null, null);"
                + "}");
            return;
        } else if (result == BrowserSwitchResult.OK) {
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
            } catch (JSONException ignored) {}

            payload = json.toString();
        } else if (result == BrowserSwitchResult.ERROR) {
            error = "new Error('" + result.getErrorMessage() + "')";
        }

        runJavaScriptInWebView(String.format("window.popupBridge.onComplete(%s, %s);", error, payload));
    }

    @Override
    public String getReturnUrlScheme() {
        return mContext.getPackageName().toLowerCase().replace("_", "") + ".popupbridge";
    }

    @JavascriptInterface
    public String getReturnUrlPrefix() {
        return String.format("%s://%s/", getReturnUrlScheme(), POPUP_BRIDGE_URL_HOST);
    }

    @JavascriptInterface
    public void open(String url) {
        browserSwitch(1, url);

        if (mNavigationListener != null) {
            mNavigationListener.onUrlOpened(url);
        }
    }

    @JavascriptInterface
    public void sendMessage(String messageName) {
        if (mMessageListener != null) {
            mMessageListener.onMessageReceived(messageName, null);
        }
    }

    @JavascriptInterface
    public void sendMessage(String messageName, String data) {
        if (mMessageListener != null) {
            mMessageListener.onMessageReceived(messageName, data);
        }
    }

    public void setNavigationListener(PopupBridgeNavigationListener listener) {
        mNavigationListener = listener;
    }

    public void setMessageListener(PopupBridgeMessageListener listener) {
        mMessageListener = listener;
    }
}
