# Android Popup Bridge v4 (Beta) Migration Guide

See the [CHANGELOG](/CHANGELOG.md) for a complete list of changes. This migration guide outlines the basics for updating your popup bridge integration from v3 to v4.

## Setup

Add the library to your dependencies in your `build.gradle`:

```groovy
dependencies {
  implementation 'com.braintreepayments.api:popup-bridge:4.0.0-beta1'
}
```

Then, add an `intent-filter` in the `AndroidManifest.xml` to your deep link destination activity:

```xml
<activity android:name="com.company.app.MyPopupBridgeActivity">
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

If these requirements are not met, an error will be reported to the `PopupBridgeErrorListener`, if registered.

## Usage

1. Include PopupBridge in your app code:

   ```java
   package com.company.myapp;
   
   import com.braintreepayments.api.PopupBridgeClient;

   class MyWebViewActivity extends Activity {
       private WebView mWebView;
       private PopupBridgeClient mPopupBridgeClient;

       @Override
       public void onCreate() {
           // Connect your web view.
           // ...

           // ...and then attach PopupBridge.
           mPopupBridgeClient = new PopupBridgeClient(this, mWebView, "my-custom-url-scheme");
   
           // register error listener
           mPopupBridgeClient.setErrorListener(error -> showDialog(error.getMessage()));
       }
   
       @Override
       protected void onResume() {
           super.onResume();
   
           // call 'deliverResult' in onResume to capture a pending result
           mPopupBridgeClient.deliverPopupBridgeResult(this);
       }
   }
   ```

2. Use PopupBridge from the web page by writing some JavaScript:

See [README](README.md) for sample code.

3. Redirect back to the app inside of the popup:

See [README](README.md) for sample code.
   

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
