package com.braintreepayments.popupbridge.test;

import android.content.Context;
import android.webkit.ValueCallback;
import android.webkit.WebView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MockWebView extends WebView {

    public String mError;
    public String mPayload;

    public MockWebView(Context context) {
        super(context);
    }

    @Override
    public void evaluateJavascript(String script, ValueCallback<String> resultCallback) {
        String expression = "window\\.popupBridge\\.onComplete\\((.*), (.*)\\)";
        Pattern pattern = Pattern.compile(expression);
        Matcher match = pattern.matcher(script);
        if (match.find()) {
            mError = match.group(1);
            mPayload = match.group(2);
        }
    }
}
