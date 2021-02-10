package com.braintreepayments.api;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.fragment.app.FragmentActivity;

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
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class PopupBridgeClientUnitTest {

    private FragmentActivity fragmentActivity;
    private BrowserSwitchClient browserSwitchClient;

    private WeakReference<FragmentActivity> activityRef;
    private WeakReference<WebView> webViewRef;

    private MockWebView webView;

    @Before
    public void setup() {
        fragmentActivity = Robolectric.setupActivity(FragmentTestActivity.class);
        browserSwitchClient = mock(BrowserSwitchClient.class);

        webView = spy(new MockWebView(fragmentActivity));

        activityRef = new WeakReference<>(fragmentActivity);
        webViewRef = new WeakReference<WebView>(webView);
    }

    @Test
    public void constructor_whenActivityIsNull_throwsException() {
        try {
            new PopupBridgeClient(null, webView, "my-custom-url-scheme");
            fail("Should throw");
        } catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), "Activity is null");
        }
    }

    @Test
    public void constructor_whenWebViewIsNull_throwsException() {
        try {
            new PopupBridgeClient(fragmentActivity, null, "my-custom-url-scheme");
            fail("Should throw");
        } catch (IllegalArgumentException e){
            assertEquals(e.getMessage(), "WebView is null");
        }
    }

    @Test
    public void constructor_enablesJavascriptOnWebView() {
        WebSettings webSettings = mock(WebSettings.class);
        when(webView.getSettings()).thenReturn(webSettings);

        new PopupBridgeClient(activityRef, webViewRef, "my-custom-url-scheme", browserSwitchClient);

        verify(webSettings).setJavaScriptEnabled(eq(true));
    }

    @Test
    @SuppressLint("JavascriptInterface")
    public void constructor_addsJavascriptInterfaceToWebView() {
        when(webView.getSettings()).thenReturn(mock(WebSettings.class));

        PopupBridgeClient sut =
                new PopupBridgeClient(activityRef, webViewRef, "my-custom-url-scheme", browserSwitchClient);

        verify(webView).addJavascriptInterface(sut, "popupBridge");
    }

    @Test
    public void deliverPopupBridgeResult_whenNotPopupBridgeRequest_doesNotCallOnComplete() {
        BrowserSwitchResult result = mock(BrowserSwitchResult.class);
        when(result.getStatus()).thenReturn(BrowserSwitchStatus.SUCCESS);
        when(browserSwitchClient.deliverResult(fragmentActivity)).thenReturn(result);

        PopupBridgeClient sut =
                new PopupBridgeClient(activityRef, webViewRef, "my-custom-url-scheme", browserSwitchClient);

        sut.deliverPopupBridgeResult(fragmentActivity);
        assertNull(webView.mError);
        assertNull(webView.mPayload);
    }

    @Test
    public void onBrowserSwitchResult_whenCancelled_callsPopupBridgeOnCancelMethod() {
        BrowserSwitchResult result = mock(BrowserSwitchResult.class);
        when(result.getStatus()).thenReturn(BrowserSwitchStatus.CANCELED);

        Uri uri = new Uri.Builder()
                .scheme("my-custom-url-scheme")
                .authority("popupbridgev1")
                .build();
        when(result.getDeepLinkUrl()).thenReturn(uri);

        when(browserSwitchClient.deliverResult(fragmentActivity)).thenReturn(result);

        PopupBridgeClient sut =
                new PopupBridgeClient(activityRef, webViewRef, "my-custom-url-scheme", browserSwitchClient);
        sut.deliverPopupBridgeResult(fragmentActivity);

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

        when(browserSwitchClient.deliverResult(fragmentActivity)).thenReturn(result);

        PopupBridgeClient sut =
                new PopupBridgeClient(activityRef, webViewRef, "my-custom-url-scheme", browserSwitchClient);
        sut.deliverPopupBridgeResult(fragmentActivity);

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

        when(browserSwitchClient.deliverResult(fragmentActivity)).thenReturn(result);

        PopupBridgeClient sut =
                new PopupBridgeClient(activityRef, webViewRef, "my-custom-url-scheme", browserSwitchClient);
        sut.deliverPopupBridgeResult(fragmentActivity);

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

        when(browserSwitchClient.deliverResult(fragmentActivity)).thenReturn(result);

        PopupBridgeClient sut =
                new PopupBridgeClient(activityRef, webViewRef, "my-custom-url-scheme", browserSwitchClient);
        sut.deliverPopupBridgeResult(fragmentActivity);

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

        when(browserSwitchClient.deliverResult(fragmentActivity)).thenReturn(result);

        PopupBridgeClient sut =
                new PopupBridgeClient(activityRef, webViewRef, "my-custom-url-scheme", browserSwitchClient);
        sut.deliverPopupBridgeResult(fragmentActivity);

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

        when(browserSwitchClient.deliverResult(fragmentActivity)).thenReturn(result);

        PopupBridgeClient sut =
                new PopupBridgeClient(activityRef, webViewRef, "my-custom-url-scheme", browserSwitchClient);
        sut.deliverPopupBridgeResult(fragmentActivity);

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

        when(browserSwitchClient.deliverResult(fragmentActivity)).thenReturn(result);

        PopupBridgeClient sut =
                new PopupBridgeClient(activityRef, webViewRef, "my-custom-url-scheme", browserSwitchClient);
        sut.deliverPopupBridgeResult(fragmentActivity);

        assertEquals("null", webView.mError);
        JSONObject payload = new JSONObject(webView.mPayload);
        assertEquals(payload.getString("path"), "");
        assertEquals(payload.getJSONObject("queryItems").length(), 0);
    }

    @Test
    public void getReturnUrlPrefix_returnsExpectedUrlPrefix() {
        PopupBridgeClient sut =
                new PopupBridgeClient(activityRef, webViewRef, "my-custom-url-scheme", browserSwitchClient);
        assertEquals(sut.getReturnUrlPrefix(), "my-custom-url-scheme://popupbridgev1/");
    }

    @Test
    public void open_launchesActivityWithUrl() throws BrowserSwitchException {
        PopupBridgeClient sut =
                new PopupBridgeClient(activityRef, webViewRef, "my-custom-url-scheme", browserSwitchClient);

        sut.open("someUrl://");

        ArgumentCaptor<BrowserSwitchOptions> captor = ArgumentCaptor.forClass(BrowserSwitchOptions.class);
        verify(browserSwitchClient).start(same(fragmentActivity), captor.capture());

        BrowserSwitchOptions browserSwitchOptions = captor.getValue();
        assertEquals(Uri.parse("someUrl://"), browserSwitchOptions.getUrl());
        assertEquals(1, browserSwitchOptions.getRequestCode());
    }

    @Test
    public void open_whenBrowserSwitchStartError_forwardsErrorToErrorListener() throws BrowserSwitchException {
        BrowserSwitchException error = new BrowserSwitchException("Browser switch error");
        doThrow(error).when(browserSwitchClient).start(same(fragmentActivity), any(BrowserSwitchOptions.class));

        PopupBridgeClient sut =
                new PopupBridgeClient(activityRef, webViewRef, "my-custom-url-scheme", browserSwitchClient);

        PopupBridgeErrorListener errorListener = mock(PopupBridgeErrorListener.class);
        sut.setErrorListener(errorListener);

        sut.open("someUrl://");
        verify(errorListener).onError(same(error));
    }

    @Test
    public void open_forwardsUrlToNavigationListener() {
        PopupBridgeClient sut =
                new PopupBridgeClient(activityRef, webViewRef, "my-custom-url-scheme", browserSwitchClient);

        PopupBridgeNavigationListener navigationListener = mock(PopupBridgeNavigationListener.class);
        sut.setNavigationListener(navigationListener);

        String url = "someUrl://";
        sut.open(url);
        verify(navigationListener).onUrlOpened(same(url));
    }

    @Test
    public void sendMessage_forwardsMessageToMessageListener() {
        PopupBridgeClient sut =
                new PopupBridgeClient(activityRef, webViewRef, "my-custom-url-scheme", browserSwitchClient);

        PopupBridgeMessageListener messageListener = mock(PopupBridgeMessageListener.class);
        sut.setMessageListener(messageListener);

        sut.sendMessage("test-message");
        verify(messageListener).onMessageReceived(eq("test-message"), (String) isNull());
    }

    @Test
    public void sendMessage_withData_forwardsMessageToMessageListener() {
        PopupBridgeClient sut =
                new PopupBridgeClient(activityRef, webViewRef, "my-custom-url-scheme", browserSwitchClient);

        PopupBridgeMessageListener messageListener = mock(PopupBridgeMessageListener.class);
        sut.setMessageListener(messageListener);

        sut.sendMessage("test-message", "data-string");
        verify(messageListener).onMessageReceived(eq("test-message"), eq("data-string"));
    }
}
