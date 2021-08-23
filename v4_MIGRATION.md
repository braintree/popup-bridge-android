# Android Popup Bridge v4 (Beta) Migration Guide

See the [CHANGELOG](/CHANGELOG.md) for a complete list of changes. This migration guide outlines the basics for updating your popup bridge integration from v3 to v4.

## Setup

First, update the popup bridge dependency version in your `build.gradle` file:

```groovy
dependencies {
  implementation 'com.braintreepayments.api:popup-bridge:4.0.1'
}
```

> In v3, `com.braintreepayments.popupbridge.PopupBridgeActivity` was the designated deep link destination activity maintained by the Braintree SDK. In v4, we've removed `PopupBridgeActivity` to give apps more control over their deep link configuration.

Next, in the `AndroidManifest.xml`, migrate the `intent-filter` from your v3 integration into an activity you own:

```xml
<activity android:name="com.company.app.MyPopupBridgeActivity"
    android:exported="true">
    ...
    <intent-filter>
        <action android:name="android.intent.action.VIEW"/>
        <data android:scheme="my-custom-url-scheme"/>
        <category android:name="android.intent.category.DEFAULT"/>
        <category android:name="android.intent.category.BROWSABLE"/>
    </intent-filter>
</activity>
```

**Note**: The scheme you define must use all lowercase letters. This is due to [scheme matching on the Android framework being case sensitive, expecting lower case](https://developer.android.com/guide/topics/manifest/data-element#scheme).
**Note**: `android:exported` is required if your app compile SDK version is API 31 (Android 12) or later.

## Usage

To use PopupBridge, instantiate a `PopupBridgeClient`:

```java
package com.company.myapp;

import com.braintreepayments.api.PopupBridgeClient;

class MyWebViewActivity extends Activity {

  private WebView webView;
  private PopupBridgeClient popupBridgeClient;
  
  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    popupBridgeClient = new PopupBridgeClient(this, webView, "my-custom-url-scheme");
  
    // register error listener
    popupBridgeClient.setErrorListener(error -> showDialog(error.getMessage()));
  }
  
  @Override
  protected void onResume() {
    super.onResume();
  
    // call 'deliverResult' in onResume to capture a pending result
    popupBridgeClient.deliverPopupBridgeResult(this);
  }
}
```

## Launch Modes

If your deep link destination activity is configured in the `AndroidManifest.xml` with `android:launchMode="singleTop"`, 
`android:launchMode="singleTask"` or `android:launchMode="singleInstance"` add the following code snippet:

```java
package com.company.app;

public class MyWebViewActivity extends AppCompatActivity {
  ... 

  @Override
  protected void onNewIntent(Intent newIntent) {
    super.onNewIntent(newIntent);
    setIntent(newIntent);
  }
}
```
