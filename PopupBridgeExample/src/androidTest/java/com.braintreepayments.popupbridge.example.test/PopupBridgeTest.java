package com.braintreepayments.popupbridge.example.test;

import android.os.Build;
import android.support.test.runner.AndroidJUnit4;

import com.lukekorth.deviceautomator.DeviceAutomator;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.lukekorth.deviceautomator.AutomatorAction.click;
import static com.lukekorth.deviceautomator.DeviceAutomator.onDevice;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withContentDescription;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withText;

@RunWith(AndroidJUnit4.class)
public class PopupBridgeTest {

    private static final long BROWSER_TIMEOUT = 20000;

    @Before
    public void setup() {
        onDevice().onHomeScreen().launchApp("com.braintreepayments.popupbridge.example");
    }

    @Test(timeout = 30000)
    public void opensPopup_whenClickingRed_returnsRedColor() {
       testColor("Red");
    }

    @Test(timeout = 30000)
    public void opensPopup_whenClickingGreen_returnsGreenColor() {
        testColor("Green");
    }

    @Test(timeout = 30000)
    public void opensPopup_whenClickingBlue_returnsBlueColor() {
        testColor("Blue");
    }

    @Test(timeout = 30000)
    public void opensPopup_whenClickingDontLikeAnyOfTheseColors_returnsCanceledSelection() {
        onViewWithText("Launch Popup").perform(click());
        onViewWithText("I don't like any of these colors")
                .waitForExists(BROWSER_TIMEOUT).perform(click());
        onViewWithText("You do not like any of these colors").waitForExists();
    }

    @Test(timeout = 30000)
    public void opensPopup_whenClickingBack_returnsCanceledSelection() {
        onViewWithText("Launch Popup").perform(click());
        onViewWithText("I don't like any of these colors")
                .waitForExists(BROWSER_TIMEOUT);
        onDevice().pressBack();
        onViewWithText("You did not choose a color").waitForExists();
    }

    private void testColor(String color) {
        onViewWithText("Launch Popup").perform(click());
        onViewWithText(color).waitForExists(BROWSER_TIMEOUT).perform(click());
        onViewWithText("Your favorite color:").waitForExists();
        onViewWithText(color.toLowerCase()).waitForExists();
    }

    private DeviceAutomator onViewWithText(String text) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return onDevice(withText(text));
        }
        return onDevice(withContentDescription(text));
    }
}
