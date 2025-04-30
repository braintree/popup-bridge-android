package com.braintreepayments.api.internal

/**
 * This class is responsible for holding parameters that are sent with analytic events.
 */
internal class AnalyticsParamRepository(
    private val uuidHelper: UUIDHelper = UUIDHelper()
) {

    private lateinit var _sessionId: String

    /**
     * Session ID to tie analytics events together which is used for reporting conversion funnels.
     */
    val sessionId: String
        get() {
            if (!this::_sessionId.isInitialized) {
                _sessionId = uuidHelper.formattedUUID
            }
            return _sessionId
        }

    /**
     * Resets the [sessionId] and clears all other repository values.
     */
    fun reset() {
        _sessionId = uuidHelper.formattedUUID
    }

    companion object {

        /**
         * Singleton instance of the AnalyticsParamRepository.
         */
        val instance: AnalyticsParamRepository by lazy { AnalyticsParamRepository() }
    }
}
