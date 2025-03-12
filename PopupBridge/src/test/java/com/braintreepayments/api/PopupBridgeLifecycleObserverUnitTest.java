package com.braintreepayments.api;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.net.Uri;

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.LooperMode;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
public class PopupBridgeLifecycleObserverUnitTest {

    private FragmentActivity activity;
    private BrowserSwitchClient browserSwitchClient;

    Function1<@NotNull BrowserSwitchResult, @NotNull Unit> onBrowserSwitchResult;

    private final String returnUrlScheme = "custom.url.scheme";
    private final Uri browserSwitchUrl = Uri.parse("https://www.example.com");

    @Before
    public void beforeEach() {
        activity = new FragmentActivity();
        browserSwitchClient = mock(BrowserSwitchClient.class);
        onBrowserSwitchResult = mock();
    }

    @Test
    public void onStateChanged_whenBrowserSwitchResultExists_popupBridgeClientDeliversBrowserSwitchResult() {
        int requestCode = PopupBridgeClient.REQUEST_CODE;
        BrowserSwitchResult browserSwitchResult =
            createBrowserSwitchResult(requestCode, browserSwitchUrl, returnUrlScheme);

        when(browserSwitchClient.getResult(activity)).thenReturn(browserSwitchResult);
        when(browserSwitchClient.deliverResult(activity)).thenReturn(browserSwitchResult);

        PopupBridgeLifecycleObserver sut = new PopupBridgeLifecycleObserver(browserSwitchClient);
        sut.setOnBrowserSwitchResult(onBrowserSwitchResult);
        sut.onStateChanged(activity, Lifecycle.Event.ON_RESUME);

        verify(onBrowserSwitchResult).invoke(browserSwitchResult);
    }

    @Test
    public void onStateChanged_whenBrowserSwitchResultIsNotAPopupBridgeResult_doesNothing() {
        int requestCode = 123;
        BrowserSwitchResult browserSwitchResult =
            createBrowserSwitchResult(requestCode, browserSwitchUrl, returnUrlScheme);

        when(browserSwitchClient.getResult(activity)).thenReturn(browserSwitchResult);
        when(browserSwitchClient.deliverResult(activity)).thenReturn(browserSwitchResult);

        PopupBridgeLifecycleObserver sut = new PopupBridgeLifecycleObserver(browserSwitchClient);
        sut.onStateChanged(activity, Lifecycle.Event.ON_RESUME);
        sut.setOnBrowserSwitchResult(onBrowserSwitchResult);

        verify(onBrowserSwitchResult, never()).invoke(browserSwitchResult);
    }

    private static BrowserSwitchResult createBrowserSwitchResult(int requestCode, Uri browserSwitchUrl, String returnUrlScheme) {
        JSONObject metadata = new JSONObject();
        BrowserSwitchRequest browserSwitchRequest =
            new BrowserSwitchRequest(requestCode, browserSwitchUrl, metadata, returnUrlScheme, true);

        Uri deepLinkUri = Uri.parse(String.format("%s://response/success", returnUrlScheme));
        return new BrowserSwitchResult(BrowserSwitchStatus.SUCCESS, browserSwitchRequest, deepLinkUri);
    }
}