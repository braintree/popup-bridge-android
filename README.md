Popup Bridge
============

Want to open a popup in a webview? Use Popup Bridge.

TODO: screencast

Installation
------------

TODO

Quick start
-----------

1. Register a custom URL scheme. TODO

1. Include a Popup Bridge in the native code.

   ```java
   import com.braintreepayments.opensource.popupbridge;

   class MyActivity extends Activity {
     @Override
     public void onCreate() {
       // Get a reference to your webview...
       WebView myWebView = TODO;

       // ...and then attach Popup Bridge.
       PopupBridge.newInstance(this, myWebView);
     }
   }
   ```

1. Use the Popup Bridge from JavaScript in place of `window.open`.

   ```javascript
   var url = 'http://localhost:4567/';

   if (window.popupBridge) {
     popupBridge.open(url + '?popupBridgereturnUrlPrefix=' + popupBridge.getReturnUrlPrefix());

     popupBridge.onComplete = function (err, payload) {
       if (err) {
         throw new Error('User closed popup.');
       }

       alert('Your favorite color is ' + payload.color);
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

1. Redirect back to the app inside of the popup.

   ```html
   <h1>What is your favorite color?</h1>

   <a href="#" data-color="red">Red</a>
   <a href="#" data-color="green">Green</a>
   <a href="#" data-color="blue">Blue</a>

   <script src="jquery.js"></script>
   <script>
   $('a').on('click', function (event) {
     var color = $(this).data('color');

     if (location.search.indexOf('popupBridgereturnUrlPrefix') !== -1) {
       var prefix = location.search.split('popupBridgereturnUrlPrefix=')[1];
       location.href = prefix + '/color=' + color;
     } else {
       window.opener.postMessage(JSON.stringify({ color: color }), '*');
     }
   });
   </script>
   ```
