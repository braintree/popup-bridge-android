package com.braintreepayments.api;

import static androidx.lifecycle.Lifecycle.Event.ON_RESUME;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;

import java.lang.ref.WeakReference;

class PopupBridgeLifecycleObserver implements LifecycleEventObserver {

    @VisibleForTesting
    final PopupBridgeClient popupBridgeClient;

    PopupBridgeLifecycleObserver(PopupBridgeClient popupBridgeClient) {
        this.popupBridgeClient = popupBridgeClient;
    }

    @Override
    public void onStateChanged(@NonNull LifecycleOwner lifecycleOwner, @NonNull Lifecycle.Event event) {
        if (event == ON_RESUME) {
            FragmentActivity activity = null;
            if (lifecycleOwner instanceof FragmentActivity) {
                activity = (FragmentActivity) lifecycleOwner;
            }

            if (activity != null) {

                /*
                 * WORKAROUND: Android 9 onResume() / onNewIntent() are called in an unpredictable way.
                 *
                 * We instruct merchants to call `setIntent(intent)` in onNewIntent so the SDK can
                 * process deep links to activities that are already running e.g. "singleTop" launch
                 * mode activities.
                 *
                 * On Android 9, onResume() can be called multiple times – once before and once after
                 * onNewIntent(). The SDK parses the deep link URI to determine if a browser-based
                 * payment flow is successful.
                 *
                 * In order to make sure the deep link intent is available to the SDK when the activity
                 * is RESUMED, we run browser switching logic on the next loop of the main thread.
                 * This prevents false negatives from occurring, where the SDK thinks the user has
                 * returned to the app without completing the flow, when in fact the deep link intent
                 * has not yet been delivered via onNewIntent.
                 */
                final WeakReference<FragmentActivity> activityRef = new WeakReference<>(activity);
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        FragmentActivity hostActivity = activityRef.get();
                        if (hostActivity != null) {
                            BrowserSwitchResult pendingResult =
                                    popupBridgeClient.getBrowserSwitchResult(hostActivity);
                            if (isPopupBridgeResult(pendingResult)) {
                                BrowserSwitchResult result =
                                        popupBridgeClient.deliverBrowserSwitchResult(hostActivity);
                                popupBridgeClient.onBrowserSwitchResult(result);
                            }
                        }
                    }
                });
            }
        }
    }

    private static boolean isPopupBridgeResult(BrowserSwitchResult result) {
        return (result != null && result.getRequestCode() == PopupBridgeClient.REQUEST_CODE);
    }
}
