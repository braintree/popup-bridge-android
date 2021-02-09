package com.braintreepayments.api;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;

import com.braintreepayments.api.test.AppCompatTestActivity;
import com.braintreepayments.api.test.FragmentTestActivity;
import com.braintreepayments.api.test.MockWebView;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class PopupBridgeTest {

    private FragmentActivity mFragmentActivity;
    private AppCompatActivity mAppCompatActivity;

    private PopupBridge mPopupBridge;
    private MockWebView mWebView;

    @Before
    public void setup() {
        mAppCompatActivity = Robolectric.setupActivity(AppCompatTestActivity.class);
        mFragmentActivity = Robolectric.setupActivity(FragmentTestActivity.class);
        mWebView = new MockWebView(mAppCompatActivity);
        mPopupBridge = PopupBridge.newInstance(mAppCompatActivity, mWebView);
    }

    @Test
    public void newInstance_whenActivityIsNull_throwsException() {
        Exception thrownException = null;
        AppCompatTestActivity appCompatTestActivity = Robolectric.setupActivity(AppCompatTestActivity.class);
        WebView webView = new WebView(appCompatTestActivity);
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
            PopupBridge.newInstance(new AppCompatTestActivity(), null);
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

        PopupBridge.newInstance(mAppCompatActivity, webView);

        verify(webSettings).setJavaScriptEnabled(eq(true));
    }

    @Test
    public void newInstance_fragmentActivity_enablesJavascriptOnWebView() {
        WebView webView = mock(WebView.class);
        WebSettings webSettings = mock(WebSettings.class);
        when(webView.getSettings()).thenReturn(webSettings);

        PopupBridge.newInstance(mFragmentActivity, webView);
        verify(webSettings).setJavaScriptEnabled(eq(true));
    }

    @Test
    @SuppressLint("JavascriptInterface")
    public void newInstance_addsJavascriptInterfaceToWebView() {
        WebView webView = mock(WebView.class);
        when(webView.getSettings()).thenReturn(mock(WebSettings.class));
        PopupBridge popupBridge = PopupBridge.newInstance(mAppCompatActivity, webView);

        verify(webView).addJavascriptInterface(eq(popupBridge), eq("popupBridge"));
    }

    @Test
    public void onBrowserSwitchResult_whenNotPopupBridgeRequest_doesNotCallOnComplete() {
        BrowserSwitchResult result = mock(BrowserSwitchResult.class);
        when(result.getStatus()).thenReturn(BrowserSwitchStatus.SUCCESS);
        mPopupBridge.onBrowserSwitchResult(result);
        assertNull(mWebView.mError);
        assertNull(mWebView.mPayload);
    }

    @Test
    public void onBrowserSwitchResult_whenCancelled_callsPopupBridgeOnCancelMethod() {
        BrowserSwitchResult result = mock(BrowserSwitchResult.class);
        when(result.getStatus()).thenReturn(BrowserSwitchStatus.CANCELED);

        Uri uri = new Uri.Builder()
                .scheme(mAppCompatActivity.getApplicationContext().getPackageName() + ".popupbridge")
                .authority("popupbridgev1")
                .build();
        when(result.getDeepLinkUrl()).thenReturn(uri);

        mPopupBridge.onBrowserSwitchResult(result);

        assertEquals(mWebView.mError, "null");
        assertEquals(mWebView.mPayload, "null");
        assertThat(mWebView.mJavascriptEval, containsString("window.popupBridge.onCancel()"));
        assertThat(mWebView.mJavascriptEval, containsString("window.popupBridge.onComplete(null, null)"));
    }

    @Test
    public void onBrowserSwitchResult_whenDifferentScheme_doesNotCallOnComplete() {
        BrowserSwitchResult result = mock(BrowserSwitchResult.class);
        when(result.getStatus()).thenReturn(BrowserSwitchStatus.SUCCESS);

        Uri uri = new Uri.Builder()
                .scheme("com.oranges.popupbridge")
                .path("mypath")
                .build();
        when(result.getDeepLinkUrl()).thenReturn(uri);

        mPopupBridge.onBrowserSwitchResult(result);

        assertNull(mWebView.mError);
        assertNull(mWebView.mPayload);
        assertNull(mWebView.mJavascriptEval);
    }

    @Test
    public void onBrowserSwitchResult_whenReturnUrlHasQueryParams_reportsPayloadWithQueryItems()
            throws JSONException {
        BrowserSwitchResult result = mock(BrowserSwitchResult.class);
        when(result.getStatus()).thenReturn(BrowserSwitchStatus.SUCCESS);

        Uri uri = new Uri.Builder()
                .scheme(mAppCompatActivity.getApplicationContext().getPackageName() + ".popupbridge")
                .authority("popupbridgev1")
                .path("mypath")
                .appendQueryParameter("foo", "bar")
                .appendQueryParameter("baz", "qux")
                .build();
        when(result.getDeepLinkUrl()).thenReturn(uri);

        mPopupBridge.onBrowserSwitchResult(result);

        assertEquals("null", mWebView.mError);
        JSONObject payload = new JSONObject(mWebView.mPayload);
        assertEquals(payload.getString("path"), "/mypath");
        assertEquals(payload.getJSONObject("queryItems").getString("foo"), "bar");
        assertEquals(payload.getJSONObject("queryItems").getString("baz"), "qux");
        assertEquals(payload.getJSONObject("queryItems").length(), 2);
        assertThat(mWebView.mJavascriptEval, containsString("window.popupBridge.onComplete(null, {"));
    }

    @Test
    public void onBrowserSwitchResult_whenReturnUrlHasNoQueryParams_reportsPayloadWithEmptyQueryItems()
            throws JSONException {
        BrowserSwitchResult result = mock(BrowserSwitchResult.class);
        when(result.getStatus()).thenReturn(BrowserSwitchStatus.SUCCESS);

        Uri uri = new Uri.Builder()
                .scheme(mAppCompatActivity.getApplicationContext().getPackageName() + ".popupbridge")
                .authority("popupbridgev1")
                .path("mypath")
                .build();
        when(result.getDeepLinkUrl()).thenReturn(uri);

        mPopupBridge.onBrowserSwitchResult(result);

        assertEquals("null", mWebView.mError);
        JSONObject payload = new JSONObject(mWebView.mPayload);
        assertEquals(payload.getString("path"), "/mypath");
        assertEquals(payload.getJSONObject("queryItems").length(), 0);
    }

    @Test
    public void  onBrowserSwitchResult_whenReturnUrlIncludesFragmentIdentifier_reportsPayloadWithFragmentIdentifier()
            throws JSONException {
        BrowserSwitchResult result = mock(BrowserSwitchResult.class);
        when(result.getStatus()).thenReturn(BrowserSwitchStatus.SUCCESS);

        Uri uri = new Uri.Builder()
                .scheme(mAppCompatActivity.getApplicationContext().getPackageName() + ".popupbridge")
                .authority("popupbridgev1")
                .fragment("hashValue")
                .build();
        when(result.getDeepLinkUrl()).thenReturn(uri);

        mPopupBridge.onBrowserSwitchResult(result);

        assertEquals("null", mWebView.mError);
        JSONObject payload = new JSONObject(mWebView.mPayload);
        assertEquals("hashValue", payload.getString("hash"));
    }

    @Test
    public void  onBrowserSwitchResult_whenReturnUrlExcludesFragmentIdentifier_fragmentIdentifierIsNotReturned()
            throws JSONException {
        BrowserSwitchResult result = mock(BrowserSwitchResult.class);
        when(result.getStatus()).thenReturn(BrowserSwitchStatus.SUCCESS);

        Uri uri = new Uri.Builder()
                .scheme(mAppCompatActivity.getApplicationContext().getPackageName() + ".popupbridge")
                .authority("popupbridgev1")
                .build();
        when(result.getDeepLinkUrl()).thenReturn(uri);

        mPopupBridge.onBrowserSwitchResult(result);

        assertEquals("null", mWebView.mError);
        JSONObject payload = new JSONObject(mWebView.mPayload);
        assertEquals("", payload.getString("path"));
        assertFalse(payload.has("hash"));
    }

    @Test
    public void onBrowserSwitchResult_whenResultIsError_reportsError() {
//        BrowserSwitchResult result = mock(BrowserSwitchResult.class);
//        when(result.getStatus()).thenReturn(BrowserSwitchStatus.CANCELED);
//        when(result.getErrorMessage()).thenReturn("Browser switch error");
//
//        mPopupBridge.onBrowserSwitchResult(result);
//
//        assertEquals("new Error('Browser switch error')", mWebView.mError);
//        assertEquals(mWebView.mJavascriptEval, "window.popupBridge.onComplete(new Error('Browser switch error'), null);");
    }

    @Test
    public void onActivityResult_whenNoPath_returnsEmptyString() throws JSONException {
        BrowserSwitchResult result = mock(BrowserSwitchResult.class);
        when(result.getStatus()).thenReturn(BrowserSwitchStatus.SUCCESS);

         Uri uri = new Uri.Builder()
                .scheme(mAppCompatActivity.getApplicationContext().getPackageName() + ".popupbridge")
                .authority("popupbridgev1")
                .build();
        when(result.getDeepLinkUrl()).thenReturn(uri);

        mPopupBridge.onBrowserSwitchResult(result);

        assertEquals("null", mWebView.mError);
        JSONObject payload = new JSONObject(mWebView.mPayload);
        assertEquals(payload.getString("path"), "");
        assertEquals(payload.getJSONObject("queryItems").length(), 0);
    }

    @Test
    public void getReturnUrlScheme_returnsExpectedUrlScheme() {
        assertEquals(mPopupBridge.getReturnUrlScheme(),
                mAppCompatActivity.getApplicationContext().getPackageName() + ".popupbridge");
    }

    @Test
    public void getReturnUrlPrefix_returnsExpectedUrlPrefix() {
        assertEquals(mPopupBridge.getReturnUrlPrefix(),
                mAppCompatActivity.getApplicationContext().getPackageName() + ".popupbridge://popupbridgev1/");
    }

    @Test
    public void open_launchesActivityWithUrl() {
        mWebView = new MockWebView(mAppCompatActivity);
        mPopupBridge = spy(PopupBridge.newInstance(mAppCompatActivity, mWebView));

        mPopupBridge.open("someUrl://");

//        verify(mPopupBridge).browserSwitch(1, "someUrl://");
    }
}
