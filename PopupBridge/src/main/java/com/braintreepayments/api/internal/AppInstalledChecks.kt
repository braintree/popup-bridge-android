package com.braintreepayments.api.internal

import android.content.Context

private const val VENMO_APP_PACKAGE = "com.venmo"

fun Context.isVenmoInstalled(): Boolean = AppHelper().isAppInstalled(this, VENMO_APP_PACKAGE)
