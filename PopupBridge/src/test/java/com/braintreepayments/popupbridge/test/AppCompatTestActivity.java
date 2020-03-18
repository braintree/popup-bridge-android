package com.braintreepayments.popupbridge.test;

import android.os.Bundle;

import com.braintreepayments.popupbridge.R;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class AppCompatTestActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.Theme_AppCompat);
    }
}
