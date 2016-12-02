package com.braintreepayments.popupbridge;

import android.app.Activity;
import android.support.test.runner.AndroidJUnit4;
import android.webkit.WebView;

import com.braintreepayments.popupbridge.test.TestActivity;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class TestPopupBridge {

    @Rule
    public final PopupBridgeActivityTestRule<TestActivity> mActivityTestRule = new PopupBridgeActivityTestRule<>(TestActivity.class);

    private Activity mActivity;

    @Before
    public void setUp() {
        mActivity = mActivityTestRule.getActivity();
    }

    @Test(timeout = 10000)
    public void newInstance_enablesJavaScript() {
        WebView webView = new WebView(mActivity);

        PopupBridge pub = PopupBridge.newInstance(null, webView);

    }

}
