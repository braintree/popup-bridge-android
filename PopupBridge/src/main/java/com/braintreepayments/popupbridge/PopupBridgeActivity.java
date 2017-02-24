package com.braintreepayments.popupbridge;

import android.app.Activity;
import android.os.Bundle;

public class PopupBridgeActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PopupBridge.sResultIntent = getIntent();
        finish();
    }
}
