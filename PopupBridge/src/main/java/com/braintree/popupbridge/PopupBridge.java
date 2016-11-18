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
import android.support.annotation.VisibleForTesting;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Set;

public class PopupBridge extends Fragment {

    private static final int POPUP_BRIDGE_REQUEST_CODE = 13592;
    private static final String TAG = "com.braintreepayments.opensource.PopupBridge";

    public static final String POPUP_BRIDGE_NAME = "PopupBridge";
    public static final String POPUP_BRIDGE_VERSION = "v1";

    @VisibleForTesting
    static final String EXTRA_BROWSER_SWITCHING = "com.braintreepayments.opensource.PopupBridge.EXTRA_BROWSER_SWITCHING";

    static Intent sResultIntent;

    private boolean mIsBrowserSwitching = false;
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

        if (intent != null) {
            JSONObject json = null;

            Uri uri = intent.getData();
            Set<String> queryParams = uri.getQueryParameterNames();

            if (queryParams != null && !queryParams.isEmpty()) {
                json = new JSONObject();
                for (String queryParam : queryParams) {
                    try {
                        json.put(queryParam, uri.getQueryParameter(queryParam));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

            if (uri.getLastPathSegment().equals("return")) {
                if (json != null) {
                    payload = json.toString();
                }
            } else {
                JSONObject errorJson = new JSONObject();
                try {
                    errorJson.put("path", uri.getPath());
                    errorJson.put("payload", json);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                error = errorJson.toString();
            }
        }

        mWebView.evaluateJavascript(String.format("PopupBridge.onComplete(%s, %s)", error, payload), null);
    }

    private static String getSchemeFromPackageName(Context context) {
        return context.getPackageName().toLowerCase().replace("_", "") + ".braintree.popupbridge";
    }

    @JavascriptInterface
    public String getReturnUrlPrefix() {
        return String.format("%s://popupbridge/%s/", getSchemeFromPackageName(mContext), POPUP_BRIDGE_VERSION);
    }

    @JavascriptInterface
    public void open(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
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

    @JavascriptInterface
    public String getVersion() {
        return POPUP_BRIDGE_VERSION;
    }
}
