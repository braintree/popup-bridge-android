# Android Popup Bridge v5 (Beta) Migration Guide

See the [CHANGELOG](/CHANGELOG.md) for a complete list of changes. This migration guide outlines the basics for updating your popup bridge integration from v4 to v5.

> When migrating from v3 to v5, make sure to view the [/v4_MIGRATION.md] first.

## Setup

First, update the popup bridge dependency version in your `build.gradle` file:

```groovy
dependencies {
  implementation 'com.braintreepayments.api:popup-bridge:5.0.0'
}
```

## Usage

The primary difference betwen v4 and v5 is the host activity no longer has to call `deliverPopupBridgeResult()` in `onResume()`. In v5, this is done internally with an Android Jetpack [Lifecycle Observer](https://developer.android.com/topic/libraries/architecture/lifecycle).

To use PopupBridge, instantiate a `PopupBridgeClient`:

```diff
package com.company.myapp;

import com.braintreepayments.api.PopupBridgeClient;

class MyWebViewActivity extends Activity {

  private WebView webView;
  private PopupBridgeClient popupBridgeClient;
  
  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    popupBridgeClient = new PopupBridgeClient(this, webView, "my-custom-url-scheme");
    popupBridgeClient.setErrorListener(error -> {
      // handle popup bridge errors
    });

    // load webpage URL
    webView.loadUrl("https://www.example.com/my_web_app");
  }

  - @Override
  - protected void onResume() {
  -   super.onResume();
  -   popupBridgeClient.deliverPopupBridgeResult(this);
  - }

  @Override
  protected void onNewIntent(Intent newIntent) {
    super.onNewIntent(newIntent);
    // (optional) for activities with a singleTop, singleTask, or singleInstance launch mode
    setIntent(newIntent);
  }
}
```
