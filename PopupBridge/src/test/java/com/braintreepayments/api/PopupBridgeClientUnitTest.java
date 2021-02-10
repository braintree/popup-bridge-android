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
import org.mockito.ArgumentCaptor;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.lang.ref.WeakReference;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class PopupBridgeClientUnitTest {

    private FragmentActivity fragmentActivity;
    private AppCompatActivity appCompatActivity;

    private PopupBridgeClient popupBridgeClient;
    private MockWebView webView;

    @Before
    public void setup() {
        appCompatActivity = Robolectric.setupActivity(AppCompatTestActivity.class);
        fragmentActivity = Robolectric.setupActivity(FragmentTestActivity.class);
        webView = new MockWebView(appCompatActivity);
        popupBridgeClient = new PopupBridgeClient(appCompatActivity, webView, "my-custom-url-scheme");
    }

    @Test
    public void constructor_whenActivityIsNull_throwsException() {
        AppCompatTestActivity appCompatTestActivity = Robolectric.setupActivity(AppCompatTestActivity.class);
        WebView webView = new WebView(appCompatTestActivity);
        try {
            popupBridgeClient = new PopupBridgeClient(null, webView, "my-custom-url-scheme");
            fail("Should throw");
        } catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), "Activity is null");
        }
    }

    @Test
    public void constructor_whenWebViewIsNull_throwsException() {
        try {
            popupBridgeClient = new PopupBridgeClient(new AppCompatTestActivity(), null, "my-custom-url-scheme");
            fail("Should throw");
        } catch (IllegalArgumentException e){
            assertEquals(e.getMessage(), "WebView is null");
        }
    }

    @Test
    public void constructor_enablesJavascriptOnWebView() {
        WebView webView = mock(WebView.class);
        WebSettings webSettings = mock(WebSettings.class);
        when(webView.getSettings()).thenReturn(webSettings);

        popupBridgeClient = new PopupBridgeClient(fragmentActivity, webView, "my-custom-url-scheme");

        verify(webSettings).setJavaScriptEnabled(eq(true));
    }

    @Test
    @SuppressLint("JavascriptInterface")
    public void constructor_addsJavascriptInterfaceToWebView() {
        WebView webView = mock(WebView.class);
        when(webView.getSettings()).thenReturn(mock(WebSettings.class));

        popupBridgeClient = new PopupBridgeClient(fragmentActivity, webView, "my-custom-url-scheme");

        verify(webView).addJavascriptInterface(eq(popupBridgeClient), eq("popupBridge"));
    }

    @Test
    public void onBrowserSwitchResult_whenNotPopupBridgeRequest_doesNotCallOnComplete() {
        BrowserSwitchResult result = mock(BrowserSwitchResult.class);
        when(result.getStatus()).thenReturn(BrowserSwitchStatus.SUCCESS);
        popupBridgeClient.onBrowserSwitchResult(result);
        assertNull(webView.mError);
        assertNull(webView.mPayload);
    }

    @Test
    public void onBrowserSwitchResult_whenCancelled_callsPopupBridgeOnCancelMethod() {
        BrowserSwitchResult result = mock(BrowserSwitchResult.class);
        when(result.getStatus()).thenReturn(BrowserSwitchStatus.CANCELED);

        Uri uri = new Uri.Builder()
                .scheme(appCompatActivity.getApplicationContext().getPackageName() + ".popupbridge")
                .authority("popupbridgev1")
                .build();
        when(result.getDeepLinkUrl()).thenReturn(uri);

        popupBridgeClient.onBrowserSwitchResult(result);

        assertEquals(webView.mError, "null");
        assertEquals(webView.mPayload, "null");
        assertThat(webView.mJavascriptEval, containsString("window.popupBridge.onCancel()"));
        assertThat(webView.mJavascriptEval, containsString("window.popupBridge.onComplete(null, null)"));
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

        popupBridgeClient.onBrowserSwitchResult(result);

        assertNull(webView.mError);
        assertNull(webView.mPayload);
        assertNull(webView.mJavascriptEval);
    }

    @Test
    public void onBrowserSwitchResult_whenReturnUrlHasQueryParams_reportsPayloadWithQueryItems()
            throws JSONException {
        BrowserSwitchResult result = mock(BrowserSwitchResult.class);
        when(result.getStatus()).thenReturn(BrowserSwitchStatus.SUCCESS);

        Uri uri = new Uri.Builder()
                .scheme("my-custom-url-scheme")
                .authority("popupbridgev1")
                .path("mypath")
                .appendQueryParameter("foo", "bar")
                .appendQueryParameter("baz", "qux")
                .build();
        when(result.getDeepLinkUrl()).thenReturn(uri);

        popupBridgeClient.onBrowserSwitchResult(result);

        assertEquals("null", webView.mError);
        JSONObject payload = new JSONObject(webView.mPayload);
        assertEquals(payload.getString("path"), "/mypath");
        assertEquals(payload.getJSONObject("queryItems").getString("foo"), "bar");
        assertEquals(payload.getJSONObject("queryItems").getString("baz"), "qux");
        assertEquals(payload.getJSONObject("queryItems").length(), 2);
        assertThat(webView.mJavascriptEval, containsString("window.popupBridge.onComplete(null, {"));
    }

    @Test
    public void onBrowserSwitchResult_whenReturnUrlHasNoQueryParams_reportsPayloadWithEmptyQueryItems()
            throws JSONException {
        BrowserSwitchResult result = mock(BrowserSwitchResult.class);
        when(result.getStatus()).thenReturn(BrowserSwitchStatus.SUCCESS);

        Uri uri = new Uri.Builder()
                .scheme("my-custom-url-scheme")
                .authority("popupbridgev1")
                .path("mypath")
                .build();
        when(result.getDeepLinkUrl()).thenReturn(uri);

        popupBridgeClient.onBrowserSwitchResult(result);

        assertEquals("null", webView.mError);
        JSONObject payload = new JSONObject(webView.mPayload);
        assertEquals(payload.getString("path"), "/mypath");
        assertEquals(payload.getJSONObject("queryItems").length(), 0);
    }

    @Test
    public void  onBrowserSwitchResult_whenReturnUrlIncludesFragmentIdentifier_reportsPayloadWithFragmentIdentifier()
            throws JSONException {
        BrowserSwitchResult result = mock(BrowserSwitchResult.class);
        when(result.getStatus()).thenReturn(BrowserSwitchStatus.SUCCESS);

        Uri uri = new Uri.Builder()
                .scheme("my-custom-url-scheme")
                .authority("popupbridgev1")
                .fragment("hashValue")
                .build();
        when(result.getDeepLinkUrl()).thenReturn(uri);

        popupBridgeClient.onBrowserSwitchResult(result);

        assertEquals("null", webView.mError);
        JSONObject payload = new JSONObject(webView.mPayload);
        assertEquals("hashValue", payload.getString("hash"));
    }

    @Test
    public void onBrowserSwitchResult_whenReturnUrlExcludesFragmentIdentifier_fragmentIdentifierIsNotReturned()
            throws JSONException {
        BrowserSwitchResult result = mock(BrowserSwitchResult.class);
        when(result.getStatus()).thenReturn(BrowserSwitchStatus.SUCCESS);

        Uri uri = new Uri.Builder()
                .scheme("my-custom-url-scheme")
                .authority("popupbridgev1")
                .build();
        when(result.getDeepLinkUrl()).thenReturn(uri);

        popupBridgeClient.onBrowserSwitchResult(result);

        assertEquals("null", webView.mError);
        JSONObject payload = new JSONObject(webView.mPayload);
        assertEquals("", payload.getString("path"));
        assertFalse(payload.has("hash"));
    }

    @Test
    public void onBrowserSwitchResult_whenNoPath_returnsEmptyString() throws JSONException {
        BrowserSwitchResult result = mock(BrowserSwitchResult.class);
        when(result.getStatus()).thenReturn(BrowserSwitchStatus.SUCCESS);

         Uri uri = new Uri.Builder()
                 .scheme("my-custom-url-scheme")
                .authority("popupbridgev1")
                .build();
        when(result.getDeepLinkUrl()).thenReturn(uri);

        popupBridgeClient.onBrowserSwitchResult(result);

        assertEquals("null", webView.mError);
        JSONObject payload = new JSONObject(webView.mPayload);
        assertEquals(payload.getString("path"), "");
        assertEquals(payload.getJSONObject("queryItems").length(), 0);
    }

    @Test
    public void getReturnUrlPrefix_returnsExpectedUrlPrefix() {
        assertEquals(popupBridgeClient.getReturnUrlPrefix(), "my-custom-url-scheme://popupbridgev1/");
    }

    @Test
    public void open_launchesActivityWithUrl() throws BrowserSwitchException {
        webView = new MockWebView(appCompatActivity);
        BrowserSwitchClient browserSwitchClient = mock(BrowserSwitchClient.class);
        popupBridgeClient = new PopupBridgeClient(new WeakReference<FragmentActivity>(appCompatActivity), new WeakReference<WebView>(webView), browserSwitchClient, "my-custom-url-scheme");

        popupBridgeClient.open("someUrl://");

        ArgumentCaptor<BrowserSwitchOptions> captor = ArgumentCaptor.forClass(BrowserSwitchOptions.class);
        verify(browserSwitchClient).start(same(appCompatActivity), captor.capture());

        BrowserSwitchOptions browserSwitchOptions = captor.getValue();
        assertEquals(Uri.parse("someUrl://"), browserSwitchOptions.getUrl());
        assertEquals(1, browserSwitchOptions.getRequestCode());
    }

    @Test
    public void open_whenBrowserSwitchStartError_forwardsErrorToErrorListener() throws BrowserSwitchException {
        webView = new MockWebView(appCompatActivity);
        BrowserSwitchClient browserSwitchClient = mock(BrowserSwitchClient.class);
        BrowserSwitchException error = new BrowserSwitchException("Browser switch error");
        doThrow(error).when(browserSwitchClient).start(same(appCompatActivity), any(BrowserSwitchOptions.class));

        popupBridgeClient = new PopupBridgeClient(new WeakReference<FragmentActivity>(appCompatActivity), new WeakReference<WebView>(webView), browserSwitchClient, "my-custom-url-scheme");

        PopupBridgeErrorListener errorListener = mock(PopupBridgeErrorListener.class);
        popupBridgeClient.setErrorListener(errorListener);

        popupBridgeClient.open("someUrl://");
        verify(errorListener).onError(same(error));
    }

    @Test
    public void open_forwardsUrlToNavigationListener() {
        webView = new MockWebView(appCompatActivity);
        BrowserSwitchClient browserSwitchClient = mock(BrowserSwitchClient.class);
        popupBridgeClient = new PopupBridgeClient(new WeakReference<FragmentActivity>(appCompatActivity), new WeakReference<WebView>(webView), browserSwitchClient, "my-custom-url-scheme");

        PopupBridgeNavigationListener navigationListener = mock(PopupBridgeNavigationListener.class);
        popupBridgeClient.setNavigationListener(navigationListener);

        String url = "someUrl://";
        popupBridgeClient.open(url);
        verify(navigationListener).onUrlOpened(same(url));
    }

    @Test
    public void sendMessage_forwardsMessageToMessageListener() {
        webView = new MockWebView(appCompatActivity);
        BrowserSwitchClient browserSwitchClient = mock(BrowserSwitchClient.class);
        popupBridgeClient = new PopupBridgeClient(new WeakReference<FragmentActivity>(appCompatActivity), new WeakReference<WebView>(webView), browserSwitchClient, "my-custom-url-scheme");

        PopupBridgeMessageListener messageListener = mock(PopupBridgeMessageListener.class);
        popupBridgeClient.setMessageListener(messageListener);

        popupBridgeClient.sendMessage("test-message");
        verify(messageListener).onMessageReceived(eq("test-message"), (String) isNull());
    }

    @Test
    public void sendMessage_withData_forwardsMessageToMessageListener() {
        webView = new MockWebView(appCompatActivity);
        BrowserSwitchClient browserSwitchClient = mock(BrowserSwitchClient.class);
        popupBridgeClient = new PopupBridgeClient(new WeakReference<FragmentActivity>(appCompatActivity), new WeakReference<WebView>(webView), browserSwitchClient, "my-custom-url-scheme");

        PopupBridgeMessageListener messageListener = mock(PopupBridgeMessageListener.class);
        popupBridgeClient.setMessageListener(messageListener);

        popupBridgeClient.sendMessage("test-message", "data-string");
        verify(messageListener).onMessageReceived(eq("test-message"), eq("data-string"));
    }
}
