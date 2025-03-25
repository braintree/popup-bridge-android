package com.braintreepayments.api.internal

import android.content.Context

private const val PAYPAL_APP_PACKAGE = "com.paypal.android.p2pmobile"
private const val VENMO_APP_PACKAGE = "com.venmo"

fun Context.isVenmoInstalled(): Boolean = AppHelper().isAppInstalled(this, VENMO_APP_PACKAGE)

fun Context.isPayPalInstalled(): Boolean = AppHelper().isAppInstalled(this, PAYPAL_APP_PACKAGE)
