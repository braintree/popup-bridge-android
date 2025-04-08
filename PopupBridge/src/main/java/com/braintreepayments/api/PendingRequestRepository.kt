package com.braintreepayments.api

/**
 * Repository responsible for storing and retrieving the pending request.
 */
class PendingRequestRepository {

    private var pendingRequest: String? = null

    fun storePendingRequest(pendingRequest: String) {
        this.pendingRequest = pendingRequest
    }

    fun getPendingRequest(): String? {
        return pendingRequest
    }

    fun clearPendingRequest() {
        pendingRequest = null
    }

}
