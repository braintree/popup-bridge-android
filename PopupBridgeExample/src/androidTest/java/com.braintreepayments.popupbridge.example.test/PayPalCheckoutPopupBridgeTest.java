package com.braintreepayments.popupbridge.example.test;

import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject2;
import android.support.test.uiautomator.UiObjectNotFoundException;

import com.braintreepayments.popupbridge.example.MainActivity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static com.lukekorth.deviceautomator.AutomatorAction.click;
import static com.lukekorth.deviceautomator.AutomatorAssertion.contentDescription;
import static com.lukekorth.deviceautomator.DeviceAutomator.onDevice;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withContentDescription;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withResourceId;
import static org.hamcrest.CoreMatchers.containsString;

@RunWith(AndroidJUnit4.class)
public class PayPalCheckoutPopupBridgeTest {

    private static final long BROWSER_TIMEOUT = 60000;
    private static final String PAYPAL_POPUPBRIDGE_EXAMPLE_URL = "https://braintree.github.io/popup-bridge-example/paypal-checkout.html";
    private static final String SANDBOX_PAYPAL_USERNAME = "sandbox-user@paypal.com";
    private static final String SANDBOX_PAYPAL_PASSWORD = "passw0rd";

    @Before
    public void setup() {
        Intent intent = InstrumentationRegistry.getContext()
                .getPackageManager()
                .getLaunchIntentForPackage("com.braintreepayments.popupbridge.example")
                .putExtra(MainActivity.EXTRA_URL, PAYPAL_POPUPBRIDGE_EXAMPLE_URL);

        onDevice().onHomeScreen().launchApp(intent);
        onDevice(withContentDescription("PayPal PopupBridge Example")).waitForExists(BROWSER_TIMEOUT);
    }

    @Test
    public void opensCheckout_returnsPaymentToken() throws UiObjectNotFoundException {
        onDevice(withContentDescription("The safer, easier way to pay")).perform(click());
        login();
        onDevice(withContentDescription("Pay Now")).perform(click());

        onDevice(withResourceId("log")).check(
                contentDescription(containsString("\"paymentToken\": \"EC-")),
                contentDescription(containsString("\"intent\": \"sale")),
                contentDescription(containsString("returnUrl"))
        );
    }

    private void login() {
        UiDevice uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        try {
            // Force a login, otherwise continue
            onDevice(withContentDescription("Not you?")).perform(click());
        } catch (RuntimeException ignored) {}

        onDevice(withContentDescription("Log In")).waitForExists(BROWSER_TIMEOUT);

        List<UiObject2> editTexts = uiDevice.findObjects(By.clazz("android.widget.EditText"));

        UiObject2 loginEditText = editTexts.get(0);
        UiObject2 passwordEditText = editTexts.get(1);

        loginEditText.setText(SANDBOX_PAYPAL_USERNAME);
        passwordEditText.setText(SANDBOX_PAYPAL_PASSWORD);

        onDevice(withContentDescription("Log In")).perform(click());
    }
}
