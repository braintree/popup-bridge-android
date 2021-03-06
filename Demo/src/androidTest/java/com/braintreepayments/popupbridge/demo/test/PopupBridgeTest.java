package com.braintreepayments.popupbridge.demo.test;

import android.os.Build;

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import com.braintreepayments.DeviceAutomator;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.braintreepayments.AutomatorAction.click;
import static com.braintreepayments.DeviceAutomator.onDevice;
import static com.braintreepayments.UiObjectMatcher.withContentDescription;
import static com.braintreepayments.UiObjectMatcher.withText;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4ClassRunner.class)
public class PopupBridgeTest {

    private static final long BROWSER_TIMEOUT = 20000;

    @Before
    public void setup() {
        onDevice().onHomeScreen().launchApp("com.braintreepayments.popupbridge.demo");
        onDevice(withContentDescription("Launch PopupBridge")).perform(click());
    }

    @Test(timeout = 50000)
    public void opensPopup_whenClickingRed_returnsRedColor() {
       testColor("Red");
    }

    @Test(timeout = 50000)
    public void opensPopup_whenClickingGreen_returnsGreenColor() {
        testColor("Green");
    }

    @Test(timeout = 50000)
    public void opensPopup_whenClickingBlue_returnsBlueColor() {
        testColor("Blue");
    }

    @Test(timeout = 50000)
    public void opensPopup_whenClickingDontLikeAnyOfTheseColors_returnsCanceledSelection() {
        onViewWithText("Launch Popup").perform(click());
        onViewWithText("I don't like any of these colors")
                .waitForExists(BROWSER_TIMEOUT).perform(click());
        assertTrue(onDevice(withText("You did not like any of our colors")).exists());
    }

    @Test(timeout = 50000)
    public void opensPopup_whenClickingBack_returnsCanceledSelection() {
        onViewWithText("Launch Popup").perform(click());
        onViewWithText("I don't like any of these colors")
                .waitForExists(BROWSER_TIMEOUT);
        onDevice().pressBack();
        assertTrue(onDevice(withText("You did not choose a color")).exists());
    }

    private void testColor(String color) {
        onViewWithText("Launch Popup").perform(click());
        onViewWithText(color).waitForExists(BROWSER_TIMEOUT).perform(click());
        onViewWithText("Your favorite color:").waitForExists();
        assertTrue(onDevice(withText(color.toLowerCase())).exists());
    }

    private DeviceAutomator onViewWithText(String text) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return onDevice(withText(text));
        }
        return onDevice(withContentDescription(text));
    }
}
