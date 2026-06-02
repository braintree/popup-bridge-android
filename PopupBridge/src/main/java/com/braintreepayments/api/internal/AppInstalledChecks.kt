package com.braintreepayments.api.internal

import android.content.Context

internal const val PAYPAL_APP_PACKAGE = "com.paypal.android.p2pmobile"
internal const val VENMO_APP_PACKAGE = "com.venmo"

internal fun Context.isPayPalInstalled(): Boolean = AppHelper().isAppInstalled(this, PAYPAL_APP_PACKAGE)
internal fun Context.isVenmoInstalled(): Boolean = AppHelper().isAppInstalled(this, VENMO_APP_PACKAGE)
