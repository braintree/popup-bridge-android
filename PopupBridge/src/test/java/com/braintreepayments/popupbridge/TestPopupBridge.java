package com.braintreepayments.popupbridge;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
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

import java.util.Collections;

import static com.braintreepayments.browserswitch.BrowserSwitchFragment.BrowserSwitchResult;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 19)
public class TestPopupBridge {

    private Activity mActivity;
    private PopupBridge mPopupBridge;
    private MockWebView mWebView;

    @Before
    public void setup() {
        mActivity = Robolectric.setupActivity(TestActivity.class);
        mWebView = new MockWebView(mActivity);
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
    public void newInstance_enablesJavascriptOnWebView() {
        WebView webView = mock(WebView.class);
        WebSettings webSettings = mock(WebSettings.class);
        when(webView.getSettings()).thenReturn(webSettings);

        PopupBridge.newInstance(mActivity, webView);

        verify(webSettings).setJavaScriptEnabled(eq(true));
    }

    @Test
    @SuppressLint("JavascriptInterface")
    public void newInstance_addsJavascriptInterfaceToWebView() {
        WebView webView = mock(WebView.class);
        when(webView.getSettings()).thenReturn(mock(WebSettings.class));
        PopupBridge popupBridge = PopupBridge.newInstance(mActivity, webView);

        verify(webView).addJavascriptInterface(eq(popupBridge), eq("popupBridge"));
    }

    @Test
    public void onBrowserSwitchResult_whenNotPopupBridgeRequest_doesNotCallOnComplete() {
        mPopupBridge.onBrowserSwitchResult(1, BrowserSwitchResult.OK, null);
        assertNull(mWebView.mError);
        assertNull(mWebView.mPayload);
    }

    @Test
    public void onBrowserSwitchResult_whenCancelled_callsPopupBridgeWithNull() {
        Uri uri = new Uri.Builder()
                .scheme(mActivity.getApplicationContext().getPackageName() + ".popupbridge")
                .authority("popupbridgev1")
                .build();

        mPopupBridge.onBrowserSwitchResult(1, BrowserSwitchResult.CANCELED, uri);

        assertEquals(mWebView.mError, "null");
        assertEquals(mWebView.mPayload, "null");
    }

    @Test
    public void onBrowserSwitchResult_whenDifferentScheme_doesNotCallOnComplete() {
        Uri uri = new Uri.Builder()
                .scheme("com.oranges.popupbridge")
                .path("mypath")
                .build();

        mPopupBridge.onBrowserSwitchResult(1, BrowserSwitchResult.OK, uri);

        assertNull(mWebView.mError);
        assertNull(mWebView.mPayload);
    }

    @Test
    public void onBrowserSwitchResult_whenReturnUrlHasQueryParams_reportsPayloadWithQueryItems()
            throws JSONException {
        Uri uri = new Uri.Builder()
                .scheme(mActivity.getApplicationContext().getPackageName() + ".popupbridge")
                .authority("popupbridgev1")
                .path("mypath")
                .appendQueryParameter("foo", "bar")
                .appendQueryParameter("baz", "qux")
                .build();

        mPopupBridge.onBrowserSwitchResult(1, BrowserSwitchResult.OK, uri);

        assertEquals("null", mWebView.mError);
        JSONObject payload = new JSONObject(mWebView.mPayload);
        assertEquals(payload.getString("path"), "/mypath");
        assertEquals(payload.getJSONObject("queryItems").getString("foo"), "bar");
        assertEquals(payload.getJSONObject("queryItems").getString("baz"), "qux");
        assertEquals(payload.getJSONObject("queryItems").length(), 2);
    }

    @Test
    public void onBrowserSwitchResult_whenReturnUrlHasNoQueryParams_reportsPayloadWithEmptyQueryItems()
            throws JSONException {
        Uri uri = new Uri.Builder()
                .scheme(mActivity.getApplicationContext().getPackageName() + ".popupbridge")
                .authority("popupbridgev1")
                .path("mypath")
                .build();

        mPopupBridge.onBrowserSwitchResult(1, BrowserSwitchResult.OK, uri);

        assertEquals("null", mWebView.mError);
        JSONObject payload = new JSONObject(mWebView.mPayload);
        assertEquals(payload.getString("path"), "/mypath");
        assertEquals(payload.getJSONObject("queryItems").length(), 0);
    }

    @Test
    public void onActivityResult_whenNoPath_returnsEmptyString() throws JSONException {
         Uri uri = new Uri.Builder()
                .scheme(mActivity.getApplicationContext().getPackageName() + ".popupbridge")
                .authority("popupbridgev1")
                .build();

        mPopupBridge.onBrowserSwitchResult(1, BrowserSwitchResult.OK, uri);

        assertEquals("null", mWebView.mError);
        JSONObject payload = new JSONObject(mWebView.mPayload);
        assertEquals(payload.getString("path"), "");
        assertEquals(payload.getJSONObject("queryItems").length(), 0);
    }

    @Test
    public void getReturnUrlScheme_returnsExpectedUrlScheme() {
        assertEquals(mPopupBridge.getReturnUrlScheme(),
                mActivity.getApplicationContext().getPackageName() + ".popupbridge");
    }

    @Test
    public void getReturnUrlPrefix_returnsExpectedUrlPrefix() {
        assertEquals(mPopupBridge.getReturnUrlPrefix(),
                mActivity.getApplicationContext().getPackageName() + ".popupbridge://popupbridgev1/");
    }

    @Test
    public void open_launchesActivityWithUrl() {
        Context context = mock(Context.class);
        when(context.getPackageName()).thenReturn("com.braintreepayments.popupbridge");
        PackageManager packageManager = mock(PackageManager.class);
        when(packageManager.queryIntentActivities(any(Intent.class), anyInt()))
                .thenReturn(Collections.singletonList(new ResolveInfo()));
        when(context.getPackageManager()).thenReturn(packageManager);
        mActivity = spy(Robolectric.setupActivity(TestActivity.class));
        when(mActivity.getApplicationContext()).thenReturn(context);
        mWebView = new MockWebView(mActivity);
        mPopupBridge = PopupBridge.newInstance(mActivity, mWebView);

        mPopupBridge.open("someUrl://");

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(context).startActivity(captor.capture());
        Uri intentUri = captor.getValue().getData();
        assertEquals(intentUri.toString(), "someUrl://");
    }
}
