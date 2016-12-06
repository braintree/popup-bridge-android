package com.braintreepayments.popupbridge;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.braintreepayments.popupbridge.test.MockWebView;
import com.braintreepayments.popupbridge.test.TestActivity;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 19)
public class TestPopupBridge {

    private PopupBridge mPopupBridge;
    private Activity mActivity;
    private MockWebView mWebView;
    private Context mContext;

    @Before
    public void setup() {
        mActivity = Robolectric.buildActivity(TestActivity.class)
                .create()
                .get();
        mActivity = spy(mActivity);
        mWebView = new MockWebView(mActivity);
        mContext = spy(mActivity.getApplicationContext());
        when(mActivity.getApplicationContext()).thenReturn(mContext);
        mPopupBridge = PopupBridge.newInstance(mActivity, mWebView);
    }

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

    @Test
    public void newInstance_enablesJavascriptOnWebview() {
        WebView webView = mock(WebView.class);
        WebSettings webSettings = mock(WebSettings.class);
        when(webView.getSettings()).thenReturn(webSettings);

        PopupBridge.newInstance(mActivity, webView);

        verify(webSettings).setJavaScriptEnabled(eq(true));
    }

    @Test
    @SuppressLint("JavascriptInterface")
    public void newInstance_addsJavascriptInterfaceToWebview() {
        WebView webView = mock(WebView.class);
        when(webView.getSettings()).thenReturn(mock(WebSettings.class));
        PopupBridge popupBridge = PopupBridge.newInstance(mActivity, webView);

        verify(webView).addJavascriptInterface(eq(popupBridge),
                eq(PopupBridge.POPUP_BRIDGE_NAME));
    }

    @Test
    public void onActivityResult_whenNotPopupBridgeRequest_doesNotCallOnComplete() {
        mPopupBridge.onActivityResult(100, Activity.RESULT_OK, null);
        assertNull(mWebView.mError);
        assertNull(mWebView.mPayload);
    }

    @Test
    public void onActivityResult_whenDifferentScheme_doesNotCallOnComplete() {
        Uri uri = new Uri.Builder()
                .scheme("com.oranges.popupbridge")
                .path("mypath")
                .build();
        Intent intent = new Intent();
        intent.setData(uri);

        mPopupBridge.onActivityResult(PopupBridge.POPUP_BRIDGE_REQUEST_CODE,
                Activity.RESULT_OK,
                intent);

        assertNull(mWebView.mError);
        assertNull(mWebView.mPayload);
    }

    @Test
    public void onActivityResult_whenSuccessful_reportsPayload() throws JSONException {
        Uri uri = new Uri.Builder()
                .scheme(mActivity.getApplicationContext().getPackageName() + ".popupbridge")
                .authority("popupbridgev1")
                .path("mypath")
                .appendQueryParameter("foo", "bar")
                .build();
        Intent intent = new Intent();
        intent.setData(uri);

        mPopupBridge.onActivityResult(PopupBridge.POPUP_BRIDGE_REQUEST_CODE,
                Activity.RESULT_OK,
                intent);

        assertEquals("null", mWebView.mError);
        assertEquals(uri.toString(), mWebView.mPayload);
    }

    @Test
    public void onActivityResult_whenCanceled_() {
        fail("Unimplemented");
    }

    @Test
    public void getReturnUrlPrefix_returnsExpectedUrlPrefix() {
        assertEquals(mPopupBridge.getReturnUrlPrefix(),
                mActivity.getApplicationContext().getPackageName() + ".popupbridge://popupbridgev1/");
    }

    @Test
    public void open_launchesActivityWithUrl() {
        mPopupBridge.open("someUrl://");

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(mActivity.getApplicationContext()).startActivity(captor.capture());
        Uri intentUri = captor.getValue().getData();
        assertEquals(intentUri.toString(), "someUrl://");
    }

    @Test
    public void startActivity_whenIntentIsSet_clearsIntent() {
        mPopupBridge.sResultIntent = new Intent();
        mPopupBridge.startActivity(new Intent());
        assertNull(mPopupBridge.sResultIntent);
    }
}
