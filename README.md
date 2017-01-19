PopupBridge
============

PopupBridge is an Android library that allows WebViews to open popup windows in a browser and send data back to the WebView.

PopupBridge is also available for [iOS](../../../popup-bridge-ios).

See the [Frequently Asked Questions](#frequently-asked-questions) to learn more about PopupBridge.

Requirements
------------

- Android SDK 19

Installation
------------

```groovy
dependencies {
  compile 'com.braintreepayments:popupbridge:0.1.+'
}
```

Quick Start
-----------

1. Add PopupBridgeActivity to `AndroidManifest.xml` and register a custom URL scheme:

  ```xml
  <activity android:name="com.braintreepayments.popupbridge.PopupBridgeActivity"
      android:launchMode="singleTask">
      <intent-filter>
          <action android:name="android.intent.action.VIEW" />
          <category android:name="android.intent.category.DEFAULT" />
          <category android:name="android.intent.category.BROWSABLE" />
          <data android:scheme="${applicationId}.popupbridge" />
      </intent-filter>
  </activity>
  ```

2. Include PopupBridge in your app code:

   ```java
   import com.braintreepayments.popupbridge.PopupBridge;

   class MyWebViewActivity extends Activity {
       private WebView mWebView;

       @Override
       public void onCreate() {
           // Connect your web view.
           // ...

           // ...and then attach PopupBridge.
           PopupBridge.newInstance(this, mWebView);
       }
   }
   ```

3. Use PopupBridge from the web page by writing some JavaScript:

   ```javascript
   var url = 'http://localhost:4567/';

   if (window.popupBridge) {
     // Open the popup in a browser, and give it the deep link back to the app
     popupBridge.open(url + '?popupBridgeReturnUrlPrefix=' + popupBridge.getReturnUrlPrefix());

     // Optional: define a callback to process results of interaction with the popup
     popupBridge.onComplete = function (err, payload) {
       if (err) {
         console.error('PopupBridge onComplete Error:', err);
       } else if (!err && !payload) {
         console.log('User closed popup.');
       } else {
         alert('Your favorite color is ' + payload.queryItems.color);
       }
     };
   } else {
     var popup = window.open(url);

     window.addEventListener('message', function (event) {
       var color = JSON.parse(event.data).color;

       if (color) {
         popup.close();
         alert('Your favorite color is ' + color);
       }
     });
   }
   ```

4. Redirect back to the app inside of the popup:

   ```html
   <h1>What is your favorite color?</h1>

   <a href="#" data-color="red">Red</a>
   <a href="#" data-color="green">Green</a>
   <a href="#" data-color="blue">Blue</a>

   <script src="jquery.js"></script>
   <script>
   $('a').on('click', function (event) {
     var color = $(this).data('color');

     if (location.search.indexOf('popupBridgeReturnUrlPrefix') !== -1) {
       var prefix = location.search.split('popupBridgereturnUrlPrefix=')[1];
       // Open the deep link back to the app, and send some data
       location.href = prefix + '?color=' + color;
     } else {
       window.opener.postMessage(JSON.stringify({ color: color }), '*');
     }
   });
   </script>
   ```

Frequently Asked Questions
--------------------------

### What does PopupBridge do?

PopupBridge allows Android apps to simulate the opening of a popup window from a WebView by opening the popup URL in an Android web browser instead.

It also allows the simulated popup window to send data back to the parent page.

### Why use PopupBridge?

By default, WebViews cannot open popups -- `window.open` will not work.

You can use [`setSupportMultipleWindows()`](https://developer.android.com/reference/android/webkit/WebSettings.html#setSupportMultipleWindows(boolean)) and [roll your own WebChromeClient and manage the windows yourself](http://maurizionapoleoni.com/blog/opening-a-window-open-in-android-without-killing-the-content-of-the-main-webview/), but this does not allow popups to communicate back to the parent WebView.

### What are some use cases for using PopupBridge?

- Apps with WebViews that need to open a popup
- When a popup window needs to to send data from the popup back to the WebView
- When the popup window needs to display the HTTPS lock icon to increase user trust
- Apps that use OAuth

### How does it work?

- PopupBridge attaches to a WebView through the [Android JavaScript interface](https://developer.android.com/reference/android/webkit/WebView.html#addJavascriptInterface(java.lang.Object, java.lang.String))
  - This exposes a JavaScript interface (via `window.popupBridge`) for the web page to interact with the Android app code
- The web page detects whether the page has access to `window.popupBridge`; if so, it uses `popupBridge.open` to open the popup URL
  - `popupBridge.open` creates an Intent to open the popup URL, which Android forwards to the user's selected browser
  - The web page can also use `popupBridge.onComplete` as a callback
- The popup web page uses a deep link URL to return back to the app
  - The deep link is in the format of `${applicationId}.popupbridge`, which is registered as a custom URL scheme in `AndroidManifest.xml`
  - One way to avoid hard-coding the deep link is by adding it as a query parameter to the popup URL:

    ```javascript
      popupBridge.open(url + '?popupBridgeReturnUrlPrefix=' + popupBridge.getReturnUrlPrefix());
    ```

    - Optionally, you can add path components and query parameters to the deep link URL to return data to the parent page, which are provided in the payload of `popupBridge.onComplete`
- If the user hits the back button or manually navigates back to the app, `popupBridge.onComplete` gets called with the error and payload as `null`

### Who built PopupBridge?

We are a team of engineers who work on the Developer Experience team at [Braintree](https://www.braintreepayments.com).

### Why did Braintree build PopupBridge?

Short answer: to accept PayPal as a payment option when mobile apps are using a WebView to power the checkout process.

PayPal used to support authentication via a modal iframe, but authentication now occurs in a popup window to increase user confidence that their account information is protected from malicious actors (the address bar shows `https://checkout.paypal.com` with the HTTPS lock icon). However, this causes issues with Braintree merchants who use a web page to power payments within their apps: they can't accept PayPal because WebViews cannot open popups and return the PayPal payment authorization data to the parent checkout page.

PopupBridge solves this problem by allowing [`braintree-web`](https://github.com/braintree/braintree-web) to open the PayPal popup from a secure mini-browser.

## Author

Braintree, code@getbraintree.com

## License

PopupBridge is available under the MIT license. See the LICENSE file for more info.
