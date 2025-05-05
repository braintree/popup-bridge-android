package com.braintreepayments.api.internal

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class AnalyticsClient(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val analyticsApi: AnalyticsApi = AnalyticsApi(context),
) {

    fun sendEvent(eventName: String) {
        coroutineScope.launch {
            analyticsApi.execute(eventName)
        }
    }
}
