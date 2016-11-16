package com.braintree.popupbridge;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Set;

public class PopupBridge extends Fragment {

    // TODO: How is this tag?
    public static final String TAG = "com.braintreepayments.opensource.PopupBridge";

    public static final String POPUP_BRIDGE_NAME = "PopupBridge";
    public static final String POPUP_BRIDGE_VERSION = "v1";

    static Intent sResultIntent;

    private WebView mWebView;
    private Context mContext;

    public PopupBridge() {
    }

    /**
     * Create a new instance of {@link PopupBridge} and add it to the {@link Activity}'s {@link FragmentManager}.
     *
     * @param activity The {@link Activity} to add the {@link Fragment} to.
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
                        } catch (IllegalStateException ignored) {
                        }
                    }
                } else {
                    fm.beginTransaction().add(popupBridge, TAG).commit();
                    try {
                        fm.executePendingTransactions();
                    } catch (IllegalStateException ignored) {
                    }
                }
            } catch (IllegalStateException e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }

        popupBridge.mContext = activity.getApplicationContext();
        popupBridge.mWebView = webView;

        popupBridge.mWebView.addJavascriptInterface(popupBridge, POPUP_BRIDGE_NAME);

        return popupBridge;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (sResultIntent == null) {
            // User switched back to app
            // TODO: consider this a form of cancellation
            return;
        }
        String error = null;
        String payload = null;
        JSONObject json = null;

        Uri data = sResultIntent.getData();
        Set<String> queryParams = data.getQueryParameterNames();

        if (queryParams != null && !queryParams.isEmpty()) {
            json = new JSONObject();
            for (String queryParam : queryParams) {
                try {
                    json.put(queryParam, data.getQueryParameter(queryParam));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        if (data.getLastPathSegment().equals("return")) {
            if (json != null) {
                payload = json.toString();
            }
        } else {
            JSONObject errorJson = new JSONObject();
            try {
                errorJson.put("path", data.getPath());
                errorJson.put("payload", json);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            error = errorJson.toString();
        }

        executeJavascript(String.format("PopupBridge.onComplete(%s, %s)",
                error,
                payload
        ), new Runnable() {
            @Override
            public void run() {
                sResultIntent = null;
            }
        });
    }

    private static String getSchemeFromPackageName(Context context) {
        return context.getPackageName().toLowerCase().replace("_", "") + ".braintree.popupbridge";
    }

    private void executeJavascript(final String javascript, final Runnable runnable) {
        mWebView.post(new Runnable() {
            @Override
            public void run() {
                if (VERSION.SDK_INT >= VERSION_CODES.KITKAT) {
                    mWebView.evaluateJavascript(javascript, new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String s) {
                            runnable.run();
                        }
                    });
                } else {
                    mWebView.loadUrl("javascript:" + javascript);
                    runnable.run();
                }
            }
        });
    }

    private void executeJavascript(final String javascript) {
        executeJavascript(javascript, new Runnable() {
            @Override
            public void run() {
            }
        });
    }

    @JavascriptInterface
    public String getReturnUrlPrefix() {
        return mContext.getString(R.string.com_braintree_popupbridge_scheme_template)
                .replace("%%SCHEME%%", getSchemeFromPackageName(mContext))
                .replace("%%VERSION%%", POPUP_BRIDGE_VERSION);
    }

    @JavascriptInterface
    public void open(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        mWebView.getContext().startActivity(intent);
    }

    @JavascriptInterface
    public String getVersion() {
        return POPUP_BRIDGE_VERSION;
    }
}
