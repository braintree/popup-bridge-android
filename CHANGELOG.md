# PopupBridge Android Release Notes

## unreleased
* Support FragmentActivity overload for PopupBridge::newInstance
* Breaking Changes
  * Update minSdkVersion to 21.
  * Update browser-switch to 1.1.1.

## 2.0.0

* Convert to AndroidX

## 1.2.0 (2018-10-08)
* Return URI fragment identifiers / URI hashes on the result object
* Added a PayPal Checkout example

## 1.1.0 (2018-09-26)
* Update SDK to 28
* Update browser-switch to 0.1.6

## 1.0.1 (2018-01-31)

* Update versionCode

## 1.0.0 (2018-01-31)

* If page has created an `popupBridge.onCancel` function, it will be called when user closes the window
* Execute JavaScript on the same thread as the WebView (fixes #10)

## 0.1.1 (2017-03-30)

* Upgrade browser-switch to 0.1.3
* Call `window.popupBridge.onComplete` with error when there is an error opening a browser

## 0.1.0 (2017-02-24)

* Initial release of PopupBridge Android
* Questions or feedback? Create an [issue](https://github.com/braintree/popup-bridge-android/issues) or [pull request](https://github.com/braintree/popup-bridge-android/pulls) on GitHub :)
