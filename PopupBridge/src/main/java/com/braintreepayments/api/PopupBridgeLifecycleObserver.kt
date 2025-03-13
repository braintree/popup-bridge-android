package com.braintreepayments.api

import android.os.Handler
import android.os.Looper
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import java.lang.ref.WeakReference

internal class PopupBridgeLifecycleObserver(
    private val browserSwitchClient: BrowserSwitchClient
) : LifecycleEventObserver {

    var onBrowserSwitchResult: ((BrowserSwitchResult) -> Unit)? = null

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (event == Lifecycle.Event.ON_RESUME) {
            var activity: FragmentActivity? = null
            if (source is FragmentActivity) {
                activity = source
            }

            if (activity != null) {
                /**
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

                val activityRef = WeakReference(activity)
                Handler(Looper.getMainLooper()).post {
                    activityRef.get()?.let { hostActivity ->
                        val pendingResult = browserSwitchClient.getResult(hostActivity)
                        if (isPopupBridgeResult(pendingResult)) {
                            val result = browserSwitchClient.deliverResult(hostActivity)
                            onBrowserSwitchResult?.invoke(result)
                        }
                    }
                }
            }
        }
    }

    companion object {
        private fun isPopupBridgeResult(result: BrowserSwitchResult?): Boolean {
            return result?.requestCode == PopupBridgeClient.REQUEST_CODE
        }
    }
}
