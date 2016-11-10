package com.braintree.popupbridge;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.Set;

public class PopupBridge extends WebViewClient {
    public static final String POPUP_BRIDGE_NAME = "PopupBridge";
    public static final String VERSION = "v1";

    static Intent sResultIntent;

    private WebView mWebView;

    public PopupBridge(WebView webView) {
        mWebView = webView;
        webView.getSettings().setJavaScriptEnabled(true);
        webView.addJavascriptInterface(this, POPUP_BRIDGE_NAME);
        mWebView.setWebViewClient(this);
        handleResult();
    }

    private void handleResult() {
        if (sResultIntent == null) {
            return;
        }
        String error = null;
        String json = null;
        String payload = null;

        Uri data = sResultIntent.getData();
        Set<String> queryParams = data.getQueryParameterNames();

        if (queryParams != null && !queryParams.isEmpty()) {
            json = "";
            for (String queryParam : queryParams) {
                json += String.format("\"%s\": \"%s\",", queryParam, data.getQueryParameter(queryParam));
            }
            json = String.format("{%s}", json.substring(0, json.length()-1));
        }

        if (data.getLastPathSegment().equals("return")) {
            payload = json;
        } else {
            error = String.format("{ \"path\": \"%s\", \"payload\": %s }", data.getPath(), json);
        }

        executeJavascript(String.format("PopupBridge.onCompleteCallback(%s, %s)",
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

    @JavascriptInterface
    public void initialize() {
        final String scheme = mWebView.getContext().getString(R.string.com_braintree_popupbridge_scheme_template)
                .replace("%%SCHEME%%", getSchemeFromPackageName(mWebView.getContext()))
                .replace("%%VERSION%%", VERSION);
        executeJavascript(String.format("PopupBridge.scheme = '%s';", scheme), new Runnable() {
            @Override
            public void run() {
                handleResult();
            }
        });
    }

    private void executeJavascript(final String javascript, final Runnable runnable) {
        mWebView.post(new Runnable() {
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
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

    @JavascriptInterface
    public void open(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        mWebView.getContext().startActivity(intent);
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        initialize();
    }
}
