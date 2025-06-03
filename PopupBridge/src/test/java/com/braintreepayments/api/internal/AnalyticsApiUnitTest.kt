package com.braintreepayments.api.internal

import com.braintreepayments.api.network.PostRequestExecutor
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.URL
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class AnalyticsApiUnitTest {

    private lateinit var postRequestExecutor: PostRequestExecutor
    private lateinit var deviceRepository: DeviceRepository
    private lateinit var analyticsParamRepository: AnalyticsParamRepository
    private lateinit var time: Time
    private lateinit var analyticsApi: AnalyticsApi

    @Before
    fun setUp() {
        postRequestExecutor = mockk(relaxed = true)
        deviceRepository = mockk(relaxed = true)
        analyticsParamRepository = mockk(relaxed = true)
        time = mockk()

        every { analyticsParamRepository.sessionId } returns "test-session-id"
        every { deviceRepository.getAppId() } returns "test-app-id"
        every { deviceRepository.getAppName() } returns "test-app-name"
        every { deviceRepository.androidApiVersion } returns "30"
        every { deviceRepository.sdkVersion } returns "1.0.0"
        every { deviceRepository.deviceManufacturer } returns "Google"
        every { deviceRepository.deviceModel } returns "Pixel 5"
        every { deviceRepository.isDeviceEmulator } returns false
        every { deviceRepository.getAppVersion() } returns "1.2.3"
        every { time.currentTime } returns 123456789L

        analyticsApi = AnalyticsApi(
            context = mockk(),
            postRequestExecutor = postRequestExecutor,
            deviceRepository = deviceRepository,
            analyticsParamRepository = analyticsParamRepository,
            time = time
        )
    }

    @Test
    fun `execute should call PostRequestExecutor with correct URL and JSON body`() = runBlocking {
        val eventName = "test-event"

        analyticsApi.execute(eventName)

        coVerify(exactly = 1) {
            postRequestExecutor.execute(
                url = URL("https://api.paypal.com/v1/tracking/batch/events"),
                jsonBody = withArg { jsonBody ->
                    val capturedJson = JSONObject(jsonBody)
                    val events = capturedJson.getJSONArray("events")
                    val eventObject = events.getJSONObject(0)
                    val batchParams = eventObject.getJSONObject("batch_params")
                    val eventParams = eventObject.getJSONArray("event_params").getJSONObject(0)

                    // Verify batch parameters
                    assertEquals("test-app-id", batchParams.getString("app_id"))
                    assertEquals("test-app-name", batchParams.getString("app_name"))
                    assertEquals("30", batchParams.getString("client_os"))
                    assertEquals("1.0.0", batchParams.getString("c_sdk_ver"))
                    assertEquals("popupbridgesdk", batchParams.getString("comp"))
                    assertEquals("Google", batchParams.getString("device_manufacturer"))
                    assertEquals("Pixel 5", batchParams.getString("mobile_device_model"))
                    assertEquals("mobile-native", batchParams.getString("event_source"))
                    assertEquals(false, batchParams.getBoolean("is_simulator"))
                    assertEquals("1.2.3", batchParams.getString("mapv"))
                    assertEquals("Android", batchParams.getString("platform"))
                    assertEquals("test-session-id", batchParams.getString("session_id"))
                    assertEquals("Braintree", batchParams.getString("tenant_name"))

                    // Verify event parameters
                    assertEquals(eventName, eventParams.getString("event_name"))
                    assertEquals(123456789L, eventParams.getLong("t")) // Verify timestamp
                }
            )
        }
    }
}
