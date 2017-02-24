package com.braintreepayments.popupbridge;

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
import android.webkit.WebView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Set;

public class PopupBridge extends Fragment {

    private static final String TAG = "com.braintreepayments.popupbridge";

    public static final int POPUP_BRIDGE_REQUEST_CODE = 13592;
    public static final String POPUP_BRIDGE_NAME = "popupBridge";
    public static final String POPUP_BRIDGE_URL_HOST = "popupbridgev1";

    static final String EXTRA_BROWSER_SWITCHING = "com.braintreepayments.popupbridge.EXTRA_BROWSER_SWITCHING";

    static Intent sResultIntent;

    private boolean mIsBrowserSwitching = false;
    private WebView mWebView;
    private Context mContext;
    private PopupBridgeNavigationListener mNavigationListener;
    private PopupBridgeMessageListener mMessageListener;

    public PopupBridge() {
    }

    /**
     * Create a new instance of {@link PopupBridge} and add it to the {@link Activity}'s {@link FragmentManager}.
     *
     * @note This will enable JavaScript in your WebView.
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

        if (mContext == null) {
            mContext = getActivity().getApplicationContext();
        }

        if (savedInstanceState != null) {
            mIsBrowserSwitching = savedInstanceState.getBoolean(EXTRA_BROWSER_SWITCHING);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mIsBrowserSwitching) {
            int resultCode = Activity.RESULT_CANCELED;
            if (sResultIntent != null) {
                resultCode = Activity.RESULT_OK;
            }

            onActivityResult(POPUP_BRIDGE_REQUEST_CODE, resultCode, sResultIntent);

            sResultIntent = null;
            mIsBrowserSwitching = false;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(EXTRA_BROWSER_SWITCHING, mIsBrowserSwitching);
    }

    @Override
    public void onActivityResult(final int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (requestCode != POPUP_BRIDGE_REQUEST_CODE) {
            return;
        }

        String error = null;
        String payload = null;


        if (requestCode != POPUP_BRIDGE_REQUEST_CODE) {
            return;
        }

        if (intent != null) {
            JSONObject json = new JSONObject();
            JSONObject queryItems = new JSONObject();

            Uri uri = intent.getData();

            if (!uri.getScheme().equals(getSchemeFromPackageName(mContext)) || !uri.getHost().equals(POPUP_BRIDGE_URL_HOST)) {
                return;
            }

            Set<String> queryParams = uri.getQueryParameterNames();

            if (queryParams != null && !queryParams.isEmpty()) {
                for (String queryParam : queryParams) {
                    try {
                        queryItems.put(queryParam, uri.getQueryParameter(queryParam));
                    } catch (JSONException e) {
                        error = "new Error('Failed to parse query items from return URL. " + e.getLocalizedMessage() + "')";
                        e.printStackTrace();
                    }
                }
            }

            try {
                json.put("path", uri.getPath());
                json.put("queryItems", queryItems);
            } catch (JSONException ignored) {}

            payload = json.toString();
        }

        mWebView.evaluateJavascript(String.format("window.popupBridge.onComplete(%s, %s);", error, payload), null);
    }

    private static String getSchemeFromPackageName(Context context) {
        return context.getPackageName().toLowerCase().replace("_", "") + ".popupbridge";
    }

    @JavascriptInterface
    public String getReturnUrlPrefix() {
        return String.format("%s://%s/", getSchemeFromPackageName(mContext), POPUP_BRIDGE_URL_HOST);
    }

    @JavascriptInterface
    public void open(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
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

    @Override
    public void startActivity(Intent intent) {
        sResultIntent = null;
        mIsBrowserSwitching = true;
        getApplicationContext().startActivity(intent);
    }

    protected Context getApplicationContext() {
        return mContext;
    }

    public void setNavigationListener(PopupBridgeNavigationListener listener) {
        mNavigationListener = listener;
    }

    public void setMessageListener(PopupBridgeMessageListener listener) {
        mMessageListener = listener;
    }
}
