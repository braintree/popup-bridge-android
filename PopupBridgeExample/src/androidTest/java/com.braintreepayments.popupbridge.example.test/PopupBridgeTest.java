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

    @Test(timeout = 20000)
    public void opensPopup_whenClickingRed_returnsRedColor() {
       testColor("Red");
    }

    @Test(timeout = 20000)
    public void opensPopup_whenClickingGreen_returnsGreenColor() {
        testColor("Green");
    }

    @Test(timeout = 20000)
    public void opensPopup_whenClickingBlue_returnsBlueColor() {
        testColor("Blue");
    }

    @Test(timeout = 20000)
    public void opensPopup_whenClickingDontLikeAnyOfTheseColors_returnsCanceledSelection() {
        onDevice(withContentDescription("Launch Popup")).perform(click());
        onDevice(withContentDescription("I don't like any of these colors")).perform(click());
        onDevice(withContentDescription("You do not like any of these colors")).waitForExists();
    }

    @Test(timeout = 20000)
    public void opensPopup_whenClickingBack_returnsCanceledSelection() {
        onDevice(withContentDescription("Launch Popup")).perform(click());
        onDevice(withContentDescription("I don't like any of these colors")).waitForExists();
        onDevice().pressBack();
        onDevice(withContentDescription("You did not choose a color")).waitForExists();
    }

    private void testColor(String color) {
        onDevice(withContentDescription("Launch Popup")).perform(click());
        onDevice(withContentDescription(color)).perform(click());
        onDevice(withContentDescription("Your favorite color:")).waitForExists();
        onDevice(withContentDescription(color.toLowerCase())).waitForExists();
    }
}
