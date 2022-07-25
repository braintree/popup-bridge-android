package com.braintreepayments.api;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class PopupDeepLinkDestinationActivity extends AppCompatActivity {

    private PopupBridgeClient mPopupBridgeClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPopupBridgeClient = new PopupBridgeClient(this, null, "com.braintreepayments.popupbridgeexample");
        mPopupBridgeClient.captureBrowserSwitchResult(this);
        setResult(AppCompatActivity.RESULT_OK);
//        finish();
    }
}
