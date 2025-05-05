package com.braintreepayments.api.internal

import android.content.Context
import com.braintreepayments.api.network.PostRequestExecutor
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL

internal class AnalyticsApi(
    context: Context,
    private val postRequestExecutor: PostRequestExecutor = PostRequestExecutor(),
    private val deviceRepository: DeviceRepository = DeviceRepository(context),
    private val analyticsParamRepository: AnalyticsParamRepository = AnalyticsParamRepository.instance,
    private val time: Time = Time(),
) {

    suspend fun execute(eventName: String) {
        postRequestExecutor.execute(
            url = URL(FPTI_URL),
            jsonBody = createJsonBody(eventName).toString()
        )
    }

    private fun createJsonBody(eventName: String): JSONObject {
        val batchParams = JSONObject().apply {
            put(KEY_APP_ID, deviceRepository.getAppId())
            put(KEY_APP_NAME, deviceRepository.getAppName())
            put(KEY_CLIENT_OS, deviceRepository.androidApiVersion)
            put(KEY_CLIENT_SDK_VERSION, deviceRepository.sdkVersion)
            put(KEY_COMPONENT, "popupbridgesdk")
            put(KEY_DEVICE_MANUFACTURER, deviceRepository.deviceManufacturer)
            put(KEY_DEVICE_MODEL, deviceRepository.deviceModel)
            put(KEY_EVENT_SOURCE, "mobile-native")
            put(KEY_IS_SIMULATOR, deviceRepository.isDeviceEmulator)
            put(KEY_MERCHANT_APP_VERSION, deviceRepository.getAppVersion())
            put(KEY_PLATFORM, "Android")
            put(KEY_SESSION_ID, analyticsParamRepository.sessionId)
            put(KEY_TENANT_NAME, "Braintree")
        }

        val eventParams = JSONArray().apply {
            put(JSONObject().apply {
                put(KEY_EVENT_NAME, eventName)
                put(KEY_TIMESTAMP, time.currentTime)

            })
        }

        val events = JSONArray().apply {
            put(JSONObject().apply {
                put("batch_params", batchParams) // No constant defined for this key
                put(KEY_EVENT_PARAMETERS, eventParams)
            })
        }

        return JSONObject().apply {
            put(KEY_EVENTS, events)
        }
    }

    companion object {
        private const val FPTI_URL = "https://api.paypal.com/v1/tracking/batch/events"

        private const val KEY_APP_ID = "app_id"
        private const val KEY_APP_NAME = "app_name"
        private const val KEY_CLIENT_SDK_VERSION = "c_sdk_ver"
        private const val KEY_CLIENT_OS = "client_os"
        private const val KEY_COMPONENT = "comp"
        private const val KEY_DEVICE_MANUFACTURER = "device_manufacturer"
        private const val KEY_DEVICE_MODEL = "mobile_device_model"
        private const val KEY_EVENT_NAME = "event_name"
        private const val KEY_EVENT_SOURCE = "event_source"
        private const val KEY_IS_SIMULATOR = "is_simulator"
        private const val KEY_MERCHANT_APP_VERSION = "mapv"
        private const val KEY_PLATFORM = "platform"
        private const val KEY_SESSION_ID = "session_id"
        private const val KEY_TIMESTAMP = "t"
        private const val KEY_TENANT_NAME = "tenant_name"

        private const val KEY_EVENT_PARAMETERS = "event_params"
        private const val KEY_EVENTS = "events"
    }
}

