package com.braintreepayments.api;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.net.Uri;

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class PopupBridgeLifecycleObserverUnitTest {

    private FragmentActivity activity;
    private PopupBridgeClient popupBridgeClient;

    private final String returnUrlScheme = "custom.url.scheme";
    private final Uri browserSwitchUrl = Uri.parse("https://www.example.com");

    @Before
    public void beforeEach() {
        activity = new FragmentActivity();
        popupBridgeClient = mock(PopupBridgeClient.class);
    }

    @Test
    public void onStateChanged_whenBrowserSwitchResultExists_popupBridgeClientDeliversBrowserSwitchResult() {
        int requestCode = PopupBridgeClient.REQUEST_CODE;
        BrowserSwitchResult browserSwitchResult =
                createBrowserSwitchResult(requestCode, browserSwitchUrl, returnUrlScheme);

        when(popupBridgeClient.getBrowserSwitchResult(activity)).thenReturn(browserSwitchResult);
        when(popupBridgeClient.deliverBrowserSwitchResult(activity)).thenReturn(browserSwitchResult);

        PopupBridgeLifecycleObserver sut = new PopupBridgeLifecycleObserver(popupBridgeClient);
        sut.onStateChanged(activity, Lifecycle.Event.ON_RESUME);

        verify(popupBridgeClient).onBrowserSwitchResult(browserSwitchResult);
    }

    @Test
    public void onStateChanged_whenBrowserSwitchResultIsNotAPopupBridgeResult_doesNothing() {
        int requestCode = 123;
        BrowserSwitchResult browserSwitchResult =
                createBrowserSwitchResult(requestCode, browserSwitchUrl, returnUrlScheme);

        when(popupBridgeClient.getBrowserSwitchResult(activity)).thenReturn(browserSwitchResult);
        when(popupBridgeClient.deliverBrowserSwitchResult(activity)).thenReturn(browserSwitchResult);

        PopupBridgeLifecycleObserver sut = new PopupBridgeLifecycleObserver(popupBridgeClient);
        sut.onStateChanged(activity, Lifecycle.Event.ON_RESUME);

        verify(popupBridgeClient, never()).onBrowserSwitchResult(browserSwitchResult);
    }

    private static BrowserSwitchResult createBrowserSwitchResult(int requestCode, Uri browserSwitchUrl, String returnUrlScheme) {
        JSONObject metadata = new JSONObject();
        BrowserSwitchRequest browserSwitchRequest =
                new BrowserSwitchRequest(requestCode, browserSwitchUrl, metadata, returnUrlScheme, true);

        Uri deepLinkUri = Uri.parse(String.format("%s://response/success", returnUrlScheme));
        return new BrowserSwitchResult(BrowserSwitchStatus.SUCCESS, browserSwitchRequest, deepLinkUri);
    }
}