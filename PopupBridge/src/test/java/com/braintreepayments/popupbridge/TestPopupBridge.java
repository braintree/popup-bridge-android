package com.braintreepayments.popupbridge;
import android.webkit.WebView;

import com.braintreepayments.popupbridge.test.TestActivity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class TestPopupBridge {

    @Test
    public void newInstance_whenActivityIsNull_throwsException() {
        Exception thrownException = null;
        TestActivity testActivity = Robolectric.setupActivity(TestActivity.class);
        WebView webView = new WebView(testActivity);
        try {
            PopupBridge.newInstance(null, webView);
        } catch (IllegalArgumentException e){
            thrownException = e;
        } finally {
            assertEquals(thrownException.getMessage(), "Activity is null");
        }
    }

    @Test
    public void newInstance_whenWebViewIsNull_throwsException() {
        Exception thrownException = null;
        try {
            PopupBridge.newInstance(new TestActivity(), null);
        } catch (IllegalArgumentException e){
            thrownException = e;
        } finally {
            assertEquals(thrownException.getMessage(), "WebView is null");
        }
    }
}
