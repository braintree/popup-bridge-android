package com.braintreepayments.popupbridge;

import android.app.Activity;
import android.support.test.rule.ActivityTestRule;

public class PopupBridgeActivityTestRule<T extends Activity> extends ActivityTestRule<T> {
    public PopupBridgeActivityTestRule(Class<T> activityClass) {
        super(activityClass);
    }
}
