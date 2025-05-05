package com.braintreepayments.api.internal

import java.util.UUID

internal class UUIDHelper {

    val formattedUUID: String
        get() = UUID.randomUUID().toString().replace("-", "")
}
