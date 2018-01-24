package com.braintreepayments.popupbridge;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

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
     * Create a new instance of {@link PopupBridge} and add it to the {@link Activity}'s {@link FragmentManager}.
     *
     * This will enable JavaScript in your WebView.
     *
     * @param activity The {@link Activity} to add the {@link Fragment} to.
     * @param webView The {@link WebView} to enable for PopupBridge.
     * @return {@link PopupBridge}
     * @throws IllegalArgumentException If the activity is not valid or the fragment cannot be added.
     */
    public static PopupBridge newInstance(Activity activity, WebView webView) throws IllegalArgumentException {
        if (activity == null) {
            throw new IllegalArgumentException("Activity is null");
        }

        if (webView == null) {
            throw new IllegalArgumentException("WebView is null");
        }

        FragmentManager fm = activity.getFragmentManager();
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

    @Override
    public void onBrowserSwitchResult(int requestCode, BrowserSwitchResult result, Uri returnUri) {
        String error = null;
        String payload = null;

        if (result == BrowserSwitchResult.OK) {
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
            } catch (JSONException ignored) {}

            payload = json.toString();
        } else if (result == BrowserSwitchResult.CANCELED) {
            //mWebView.evaluateJavascript(""
            //    + "if (typeof window.popupBridge.onClose === 'function') {"
            //    + "  window.popupBridge.onClose();"
            //    + "} else {"
            //    + "  window.popupBridge.onComplete(null, null);"
            //    + "}", null);
            //return;
        } else if (result == BrowserSwitchResult.ERROR) {
            error = "new Error('" + result.getErrorMessage() + "')";
        }

        mWebView.evaluateJavascript(String.format("window.popupBridge.onComplete(%s, %s);", error,
                payload), null);
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
