package com.braintreepayments.popupbridge;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.braintreepayments.popupbridge.test.MockWebView;
import com.braintreepayments.popupbridge.test.TestAppCompatActivity;
import com.braintreepayments.popupbridge.test.TestFragmentActivity;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.util.ReflectionHelpers;

import java.util.Collections;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;

import static com.braintreepayments.browserswitch.BrowserSwitchFragment.BrowserSwitchResult;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class PopupBridgeTest {

    private FragmentActivity mFragmentActivity;
    private PopupBridge mFragmentActivityPopupBridge;
    private MockWebView mFragmentActivityWebView;

    private AppCompatActivity mAppCompatActivity;
    private PopupBridge mAppCompatActivityPopupBridge;
    private MockWebView mAppCompatActivityWebView;

    @Before
    public void setup() {
        mFragmentActivity = Robolectric.setupActivity(TestFragmentActivity.class);
        mFragmentActivityWebView = new MockWebView(mFragmentActivity);
        mFragmentActivityPopupBridge = PopupBridge.newInstance(mFragmentActivity, mFragmentActivityWebView);

        mAppCompatActivity = Robolectric.setupActivity(TestAppCompatActivity.class);
        mAppCompatActivityWebView = new MockWebView(mAppCompatActivity);
        mAppCompatActivityPopupBridge = PopupBridge.newInstance(mAppCompatActivity, mAppCompatActivityWebView);
    }

    @Test
    public void newInstance_whenActivityIsNull_throwsException() {
        Exception thrownException = null;
        TestFragmentActivity testFragmentActivity = Robolectric.setupActivity(TestFragmentActivity.class);
        WebView webView = new WebView(testFragmentActivity);
        try {
            PopupBridge.newInstance(null, webView);
        } catch (IllegalArgumentException e){
            thrownException = e;
        } finally {
            assertEquals(thrownException.getMessage(), "Activity is null");
        }
    }

    @Test
    public void newInstanceWithFragmentActivity_whenWebViewIsNull_throwsException() {
        Exception thrownException = null;
        try {
            PopupBridge.newInstance(new TestFragmentActivity(), null);
        } catch (IllegalArgumentException e){
            thrownException = e;
        } finally {
            assertEquals(thrownException.getMessage(), "WebView is null");
        }
    }

    @Test
    public void newInstanceWithAppCompatActivity_whenWebViewIsNull_throwsException() {
        Exception thrownException = null;
        try {
            PopupBridge.newInstance(new TestAppCompatActivity(), null);
        } catch (IllegalArgumentException e){
            thrownException = e;
        } finally {
            assertEquals(thrownException.getMessage(), "WebView is null");
        }
    }

    @Test
    public void newInstanceWithFragmentActivity_enablesJavascriptOnWebView() {
        WebView webView = mock(WebView.class);
        WebSettings webSettings = mock(WebSettings.class);
        when(webView.getSettings()).thenReturn(webSettings);

        PopupBridge.newInstance(mFragmentActivity, webView);
        verify(webSettings).setJavaScriptEnabled(eq(true));
    }

    @Test
    public void newInstanceWithAppCompatActivity_enablesJavascriptOnWebView() {
        WebView webView = mock(WebView.class);
        WebSettings webSettings = mock(WebSettings.class);
        when(webView.getSettings()).thenReturn(webSettings);

        PopupBridge.newInstance(mAppCompatActivity, webView);
        verify(webSettings).setJavaScriptEnabled(eq(true));
    }

    @Test
    @SuppressLint("JavascriptInterface")
    public void newInstanceWithFragmentActivity_addsJavascriptInterfaceToWebView() {
        WebView webView = mock(WebView.class);
        when(webView.getSettings()).thenReturn(mock(WebSettings.class));
        PopupBridge popupBridge = PopupBridge.newInstance(mFragmentActivity, webView);

        verify(webView).addJavascriptInterface(eq(popupBridge), eq("popupBridge"));
    }

    @Test
    @SuppressLint("JavascriptInterface")
    public void newInstanceWithAppCompatActivity_addsJavascriptInterfaceToWebView() {
        WebView webView = mock(WebView.class);
        when(webView.getSettings()).thenReturn(mock(WebSettings.class));
        PopupBridge popupBridge = PopupBridge.newInstance(mFragmentActivity, webView);

        verify(webView).addJavascriptInterface(eq(popupBridge), eq("popupBridge"));
    }

    @Test
    public void onBrowserSwitchResultWithFragmentActivity_whenNotPopupBridgeRequest_doesNotCallOnComplete() {
        mFragmentActivityPopupBridge.onBrowserSwitchResult(1, BrowserSwitchResult.OK, null);
        assertNull(mFragmentActivityWebView.mError);
        assertNull(mFragmentActivityWebView.mPayload);
    }

    @Test
    public void onBrowserSwitchResultWithAppCompatActivity_whenNotPopupBridgeRequest_doesNotCallOnComplete() {
        mAppCompatActivityPopupBridge.onBrowserSwitchResult(1, BrowserSwitchResult.OK, null);
        assertNull(mAppCompatActivityWebView.mError);
        assertNull(mAppCompatActivityWebView.mPayload);
    }

    @Test
    public void onBrowserSwitchResultWithFragmentActivity_whenCancelled_callsPopupBridgeOnCancelMethod() {
        Uri uri = new Uri.Builder()
                .scheme(mFragmentActivity.getApplicationContext().getPackageName() + ".popupbridge")
                .authority("popupbridgev1")
                .build();

        mFragmentActivityPopupBridge.onBrowserSwitchResult(1, BrowserSwitchResult.CANCELED, uri);

        assertEquals(mFragmentActivityWebView.mError, "null");
        assertEquals(mFragmentActivityWebView.mPayload, "null");
        assertThat(mFragmentActivityWebView.mJavascriptEval, containsString("window.popupBridge.onCancel()"));
        assertThat(mFragmentActivityWebView.mJavascriptEval, containsString("window.popupBridge.onComplete(null, null)"));
    }

    @Test
    public void onBrowserSwitchResultWithAppCompatActivity_whenCancelled_callsPopupBridgeOnCancelMethod() {
        Uri uri = new Uri.Builder()
                .scheme(mFragmentActivity.getApplicationContext().getPackageName() + ".popupbridge")
                .authority("popupbridgev1")
                .build();

        mAppCompatActivityPopupBridge.onBrowserSwitchResult(1, BrowserSwitchResult.CANCELED, uri);

        assertEquals(mAppCompatActivityWebView.mError, "null");
        assertEquals(mAppCompatActivityWebView.mPayload, "null");
        assertThat(mAppCompatActivityWebView.mJavascriptEval, containsString("window.popupBridge.onCancel()"));
        assertThat(mAppCompatActivityWebView.mJavascriptEval, containsString("window.popupBridge.onComplete(null, null)"));
    }

    @Test
    public void onBrowserSwitchResultWithFragmentActivity_whenDifferentScheme_doesNotCallOnComplete() {
        Uri uri = new Uri.Builder()
                .scheme("com.oranges.popupbridge")
                .path("mypath")
                .build();

        mFragmentActivityPopupBridge.onBrowserSwitchResult(1, BrowserSwitchResult.OK, uri);

        assertNull(mFragmentActivityWebView.mError);
        assertNull(mFragmentActivityWebView.mPayload);
        assertNull(mFragmentActivityWebView.mJavascriptEval);
    }

    @Test
    public void onBrowserSwitchResultWithAppCompatActivity_whenDifferentScheme_doesNotCallOnComplete() {
        Uri uri = new Uri.Builder()
                .scheme("com.oranges.popupbridge")
                .path("mypath")
                .build();

        mAppCompatActivityPopupBridge.onBrowserSwitchResult(1, BrowserSwitchResult.OK, uri);

        assertNull(mAppCompatActivityWebView.mError);
        assertNull(mAppCompatActivityWebView.mPayload);
        assertNull(mAppCompatActivityWebView.mJavascriptEval);
    }

    @Test
    public void onBrowserSwitchResultWithFragmentActivity_whenReturnUrlHasQueryParams_reportsPayloadWithQueryItems()
            throws JSONException {
        Uri uri = new Uri.Builder()
                .scheme(mFragmentActivity.getApplicationContext().getPackageName() + ".popupbridge")
                .authority("popupbridgev1")
                .path("mypath")
                .appendQueryParameter("foo", "bar")
                .appendQueryParameter("baz", "qux")
                .build();

        mFragmentActivityPopupBridge.onBrowserSwitchResult(1, BrowserSwitchResult.OK, uri);

        assertEquals("null", mFragmentActivityWebView.mError);
        JSONObject payload = new JSONObject(mFragmentActivityWebView.mPayload);
        assertEquals(payload.getString("path"), "/mypath");
        assertEquals(payload.getJSONObject("queryItems").getString("foo"), "bar");
        assertEquals(payload.getJSONObject("queryItems").getString("baz"), "qux");
        assertEquals(payload.getJSONObject("queryItems").length(), 2);
        assertThat(mFragmentActivityWebView.mJavascriptEval, containsString("window.popupBridge.onComplete(null, {"));
    }

    @Test
    public void onBrowserSwitchResultWithAppCompatActivity_whenReturnUrlHasQueryParams_reportsPayloadWithQueryItems()
            throws JSONException {
        Uri uri = new Uri.Builder()
                .scheme(mAppCompatActivity.getApplicationContext().getPackageName() + ".popupbridge")
                .authority("popupbridgev1")
                .path("mypath")
                .appendQueryParameter("foo", "bar")
                .appendQueryParameter("baz", "qux")
                .build();

        mAppCompatActivityPopupBridge.onBrowserSwitchResult(1, BrowserSwitchResult.OK, uri);

        assertEquals("null", mAppCompatActivityWebView.mError);
        JSONObject payload = new JSONObject(mAppCompatActivityWebView.mPayload);
        assertEquals(payload.getString("path"), "/mypath");
        assertEquals(payload.getJSONObject("queryItems").getString("foo"), "bar");
        assertEquals(payload.getJSONObject("queryItems").getString("baz"), "qux");
        assertEquals(payload.getJSONObject("queryItems").length(), 2);
        assertThat(mAppCompatActivityWebView.mJavascriptEval, containsString("window.popupBridge.onComplete(null, {"));
    }

    @Test
    public void onBrowserSwitchResultWithFragmentActivity_whenReturnUrlHasNoQueryParams_reportsPayloadWithEmptyQueryItems()
            throws JSONException {
        Uri uri = new Uri.Builder()
                .scheme(mFragmentActivity.getApplicationContext().getPackageName() + ".popupbridge")
                .authority("popupbridgev1")
                .path("mypath")
                .build();

        mFragmentActivityPopupBridge.onBrowserSwitchResult(1, BrowserSwitchResult.OK, uri);

        assertEquals("null", mFragmentActivityWebView.mError);
        JSONObject payload = new JSONObject(mFragmentActivityWebView.mPayload);
        assertEquals(payload.getString("path"), "/mypath");
        assertEquals(payload.getJSONObject("queryItems").length(), 0);
    }

    @Test
    public void onBrowserSwitchResultWithAppCompatActivity_whenReturnUrlHasNoQueryParams_reportsPayloadWithEmptyQueryItems()
            throws JSONException {
        Uri uri = new Uri.Builder()
                .scheme(mAppCompatActivity.getApplicationContext().getPackageName() + ".popupbridge")
                .authority("popupbridgev1")
                .path("mypath")
                .build();

        mAppCompatActivityPopupBridge.onBrowserSwitchResult(1, BrowserSwitchResult.OK, uri);

        assertEquals("null", mAppCompatActivityWebView.mError);
        JSONObject payload = new JSONObject(mAppCompatActivityWebView.mPayload);
        assertEquals(payload.getString("path"), "/mypath");
        assertEquals(payload.getJSONObject("queryItems").length(), 0);
    }

    @Test
    public void  onBrowserSwitchResultWithFragmentActivity_whenReturnUrlIncludesFragmentIdentifier_reportsPayloadWithFragmentIdentifier()
            throws JSONException {
        Uri uri = new Uri.Builder()
                .scheme(mFragmentActivity.getApplicationContext().getPackageName() + ".popupbridge")
                .authority("popupbridgev1")
                .fragment("hashValue")
                .build();

        mFragmentActivityPopupBridge.onBrowserSwitchResult(1, BrowserSwitchResult.OK, uri);

        assertEquals("null", mFragmentActivityWebView.mError);
        JSONObject payload = new JSONObject(mFragmentActivityWebView.mPayload);
        assertEquals("hashValue", payload.getString("hash"));
    }

    @Test
    public void  onBrowserSwitchResultWithAppCompatActivity_whenReturnUrlIncludesFragmentIdentifier_reportsPayloadWithFragmentIdentifier()
            throws JSONException {
        Uri uri = new Uri.Builder()
                .scheme(mAppCompatActivity.getApplicationContext().getPackageName() + ".popupbridge")
                .authority("popupbridgev1")
                .fragment("hashValue")
                .build();

        mAppCompatActivityPopupBridge.onBrowserSwitchResult(1, BrowserSwitchResult.OK, uri);

        assertEquals("null", mAppCompatActivityWebView.mError);
        JSONObject payload = new JSONObject(mAppCompatActivityWebView.mPayload);
        assertEquals("hashValue", payload.getString("hash"));
    }

    @Test
    public void onBrowserSwitchResultWithFragmentActivity_whenReturnUrlExcludesFragmentIdentifier_fragmentIdentifierIsNotReturned()
            throws JSONException {
        Uri uri = new Uri.Builder()
                .scheme(mFragmentActivity.getApplicationContext().getPackageName() + ".popupbridge")
                .authority("popupbridgev1")
                .build();

        mFragmentActivityPopupBridge.onBrowserSwitchResult(1, BrowserSwitchResult.OK, uri);

        assertEquals("null", mFragmentActivityWebView.mError);
        JSONObject payload = new JSONObject(mFragmentActivityWebView.mPayload);
        assertEquals("", payload.getString("path"));
        assertFalse(payload.has("hash"));
    }

    @Test
    public void onBrowserSwitchResultWithAppCompatActivity_whenReturnUrlExcludesFragmentIdentifier_fragmentIdentifierIsNotReturned()
            throws JSONException {
        Uri uri = new Uri.Builder()
                .scheme(mAppCompatActivity.getApplicationContext().getPackageName() + ".popupbridge")
                .authority("popupbridgev1")
                .build();

        mAppCompatActivityPopupBridge.onBrowserSwitchResult(1, BrowserSwitchResult.OK, uri);

        assertEquals("null", mAppCompatActivityWebView.mError);
        JSONObject payload = new JSONObject(mAppCompatActivityWebView.mPayload);
        assertEquals("", payload.getString("path"));
        assertFalse(payload.has("hash"));
    }

    @Test
    public void onBrowserSwitchResultWithFragmentActivity_whenResultIsError_reportsError() {
        BrowserSwitchResult result = BrowserSwitchResult.ERROR;
        ReflectionHelpers.callInstanceMethod(result, "setErrorMessage",
                new ReflectionHelpers.ClassParameter<>(String.class, "Browser switch error"));

        mFragmentActivityPopupBridge.onBrowserSwitchResult(1, result, null);

        assertEquals("new Error('Browser switch error')", mFragmentActivityWebView.mError);
        assertEquals(mFragmentActivityWebView.mJavascriptEval, "window.popupBridge.onComplete(new Error('Browser switch error'), null);");
    }

    @Test
    public void onBrowserSwitchResultWithAppCompatActivity_whenResultIsError_reportsError() {
        BrowserSwitchResult result = BrowserSwitchResult.ERROR;
        ReflectionHelpers.callInstanceMethod(result, "setErrorMessage",
                new ReflectionHelpers.ClassParameter<>(String.class, "Browser switch error"));

        mAppCompatActivityPopupBridge.onBrowserSwitchResult(1, result, null);

        assertEquals("new Error('Browser switch error')", mAppCompatActivityWebView.mError);
        assertEquals(mAppCompatActivityWebView.mJavascriptEval, "window.popupBridge.onComplete(new Error('Browser switch error'), null);");
    }

    @Test
    public void onActivityResultWithFragmentActivity_whenNoPath_returnsEmptyString() throws JSONException {
        Uri uri = new Uri.Builder()
                .scheme(mFragmentActivity.getApplicationContext().getPackageName() + ".popupbridge")
                .authority("popupbridgev1")
                .build();

        mFragmentActivityPopupBridge.onBrowserSwitchResult(1, BrowserSwitchResult.OK, uri);

        assertEquals("null", mFragmentActivityWebView.mError);
        JSONObject payload = new JSONObject(mFragmentActivityWebView.mPayload);
        assertEquals(payload.getString("path"), "");
        assertEquals(payload.getJSONObject("queryItems").length(), 0);
    }

    @Test
    public void onActivityResultWithAppCompatActivity_whenNoPath_returnsEmptyString() throws JSONException {
         Uri uri = new Uri.Builder()
                .scheme(mAppCompatActivity.getApplicationContext().getPackageName() + ".popupbridge")
                .authority("popupbridgev1")
                .build();

        mAppCompatActivityPopupBridge.onBrowserSwitchResult(1, BrowserSwitchResult.OK, uri);

        assertEquals("null", mAppCompatActivityWebView.mError);
        JSONObject payload = new JSONObject(mAppCompatActivityWebView.mPayload);
        assertEquals(payload.getString("path"), "");
        assertEquals(payload.getJSONObject("queryItems").length(), 0);
    }

    @Test
    public void getReturnUrlSchemeWithFragmentActivity_returnsExpectedUrlScheme() {
        assertEquals(mFragmentActivityPopupBridge.getReturnUrlScheme(),
                mFragmentActivity.getApplicationContext().getPackageName() + ".popupbridge");
    }

    @Test
    public void getReturnUrlSchemeWithAppCompatActivity_returnsExpectedUrlScheme() {
        assertEquals(mAppCompatActivityPopupBridge.getReturnUrlScheme(),
                mAppCompatActivity.getApplicationContext().getPackageName() + ".popupbridge");
    }

    @Test
    public void getReturnUrlPrefixWithFragmentActivity_returnsExpectedUrlPrefix() {
        assertEquals(mFragmentActivityPopupBridge.getReturnUrlPrefix(),
                mFragmentActivity.getApplicationContext().getPackageName() + ".popupbridge://popupbridgev1/");
    }

    @Test
    public void getReturnUrlPrefixWithAppCompatActivity_returnsExpectedUrlPrefix() {
        assertEquals(mAppCompatActivityPopupBridge.getReturnUrlPrefix(),
                mAppCompatActivity.getApplicationContext().getPackageName() + ".popupbridge://popupbridgev1/");
    }

    @Test
    public void openWithFragmentActivity_launchesActivityWithUrl() {
        Context context = mock(Context.class);
        when(context.getPackageName()).thenReturn("com.braintreepayments.popupbridge");
        PackageManager packageManager = mock(PackageManager.class);
        when(packageManager.queryIntentActivities(any(Intent.class), anyInt()))
                .thenReturn(Collections.singletonList(new ResolveInfo()));
        when(context.getPackageManager()).thenReturn(packageManager);
        mFragmentActivity = spy(Robolectric.setupActivity(TestFragmentActivity.class));
        when(mFragmentActivity.getApplicationContext()).thenReturn(context);
        mFragmentActivityWebView = new MockWebView(mFragmentActivity);
        mFragmentActivityPopupBridge = PopupBridge.newInstance(mFragmentActivity, mFragmentActivityWebView);

        mFragmentActivityPopupBridge.open("someUrl://");

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(context).startActivity(captor.capture());
        Uri intentUri = captor.getValue().getData();
        assertEquals(intentUri.toString(), "someUrl://");
    }

    @Test
    public void openWithAppCompatActivity_launchesActivityWithUrl() {
        Context context = mock(Context.class);
        when(context.getPackageName()).thenReturn("com.braintreepayments.popupbridge");
        PackageManager packageManager = mock(PackageManager.class);
        when(packageManager.queryIntentActivities(any(Intent.class), anyInt()))
                .thenReturn(Collections.singletonList(new ResolveInfo()));
        when(context.getPackageManager()).thenReturn(packageManager);
        mAppCompatActivity = spy(Robolectric.setupActivity(TestAppCompatActivity.class));
        when(mAppCompatActivity.getApplicationContext()).thenReturn(context);
        mAppCompatActivityWebView = new MockWebView(mAppCompatActivity);
        mAppCompatActivityPopupBridge = PopupBridge.newInstance(mAppCompatActivity, mAppCompatActivityWebView);

        mAppCompatActivityPopupBridge.open("someUrl://");

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(context).startActivity(captor.capture());
        Uri intentUri = captor.getValue().getData();
        assertEquals(intentUri.toString(), "someUrl://");
    }
}
