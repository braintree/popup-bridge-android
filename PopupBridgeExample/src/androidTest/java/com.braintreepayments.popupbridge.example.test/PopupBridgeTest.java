package com.braintreepayments.popupbridge.example.test;

import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.lukekorth.deviceautomator.AutomatorAction.click;
import static com.lukekorth.deviceautomator.DeviceAutomator.onDevice;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withContentDescription;

@RunWith(AndroidJUnit4.class)
public class PopupBridgeTest {

    @Before
    public void setup() {
        onDevice().onHomeScreen();

        onDevice().launchApp("com.braintreepayments.popupbridge.example");
    }

    @Test(timeout = 10000)
    public void opensPopupAndReturnsWithPayload() {
        onDevice(withContentDescription("Launch Popup")).perform(click());
        onDevice(withContentDescription("Return")).perform(click());
        onDevice(withContentDescription("Payload:")).waitForExists();
        onDevice(withContentDescription("{\"a\":\"1\",\"key\":\"value\",\"payload\":\"true\",\"path\":\"/return\"}")).waitForExists();
    }

    @Test(timeout = 10000)
    public void opensPopupAndCancels() {
        onDevice(withContentDescription("Launch Popup")).perform(click());
        onDevice(withContentDescription("Cancel")).perform(click());
        onDevice(withContentDescription("Payload:")).waitForExists();
        onDevice(withContentDescription("{\"a\":\"1\",\"key\":\"value\",\"payload\":\"true\",\"path\":\"/return\"}")).waitForExists();
    }



}
