# PopupBridge Android Release Notes

## unreleased

* Fix an issue where the WebViewClient is being silently overridden. [#95](https://github.com/braintree/popup-bridge-android/issues/95)

## 5.0.0

* Android 13 Support
  * Upgrade `targetSdkVersion` and `compileSdkVersion` to API 33
* Disable Jetifier
* Update browser-switch to 3.0.0
* Breaking Changes
  * Remove `PopupBridgeClient#deliverPopupBridgeResult(FragmentActivity)`
  * Make `PopupBridgeClient#POPUP_BRIDGE_NAME` package-private
  * Make `PopupBridgeClient#POPUP_BRIDGE_URL_HOST` package-private
  * Remove `PopupBridgeLifecycleObserver`
  * Replace `FragmentActivity` with `ComponentActivity` in `PopupBridgeClient`'s constructor
* Add validation to check if the Venmo app is installed on the device.

## 4.1.0

* Upgrade `targetSdkVersion` and `compileSdkVersion` to API 31
* Bump `browser-switch` version to `2.1.0`

## 4.0.1

* Upgrade browser-switch to 2.0.1

## 4.0.0

* Upgrade browser-switch to 2.0.0

**Note:** Includes all changes in [4.0.0-beta1](#400-beta1), [4.0.0-beta2](#400-beta2), and [4.0.0-beta3](#400-beta3)

## 4.0.0-beta3

* Upgrade browser-switch to 2.0.0-beta3

## 3.1.1

* Upgrade browser-switch to 1.1.4

## 4.0.0-beta2

* Update browser-switch to 2.0.0-beta2
* Run `onComplete` and `onCancel` callbacks after WebView window has loaded (fixes #26)

## 4.0.0-beta1

* Add `PopupBridgeClient`
* Add `PopupBridgeErrorListener`
* Breaking Changes
  * Update browser-switch to 2.0.0-beta1
  * Change package from `com.braintreepayments` to `com.braintreepayments.api`
  * Remove `PopupBridge`
  * Remove `PopupBridgeActivity`

## 3.1.0

* Bump `compileSdkVersion` and `targetSdkVersion` to API level 30
* Upgrade browser-switch to 1.1.2

## 3.0.0

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

