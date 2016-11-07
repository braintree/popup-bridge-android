package com.braintree.popupbridge.example;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.braintree.popupbridge.PopupBridge;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("PopupBridge Version", new PopupBridge().version);
    }
}
