# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build
./gradlew build
./gradlew assemble                        # Produce AAR/APK artifacts

# Unit tests
./gradlew testRelease                     # All unit tests
./gradlew PopupBridge:testRelease         # Library unit tests only

# Instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Lint & static analysis
./gradlew lint                            # Android Lint
./gradlew detekt                          # Detekt static analysis
./gradlew detekt --auto-correct           # Auto-fix Detekt issues
./ci lint                                 # Lint + Detekt (CI mode)
./ci lint-check                           # Lint only (no auto-correct)
```

## Architecture

**popup-bridge-android** is an Android library (`com.braintreepayments.api:popup-bridge`) that lets a WebView open an external browser popup and receive the result back via deep link, using a JavaScript bridge.

### Module structure

| Module | Purpose |
|--------|---------|
| `PopupBridge/` | Main library (published AAR) |
| `Demo/` | Example app demonstrating library usage |

### Core flow

1. **Host app** creates a `PopupBridgeClient` with an `Activity`, a `WebView`, and a URL scheme (e.g. `my-app://`).
2. `PopupBridgeClient` injects a `PopupBridgeJavascriptInterface` into the WebView, exposing `window.popupBridge.open(url)` and `window.popupBridge.getReturnUrlPrefix()` to web content.
3. When JavaScript calls `popupBridge.open()`, the library opens the URL in an external browser via `BrowserSwitchClient` (Braintree's `browser-switch` library).
4. When the external browser redirects back to the host app's URL scheme, the deep link is intercepted and the payload is delivered to the web page via `window.popupBridge.onComplete()`.
5. Analytics events (started / succeeded / failed / canceled) are sent to a Braintree backend via `AnalyticsClient`.

### Key public API surface (`PopupBridge/src/main/`)

| Class | Role |
|-------|------|
| `PopupBridgeClient` | Single entry point — owns the `BrowserSwitchClient`, registers the JS interface, and handles deep link results |
| `PopupBridgeWebViewClient` | Custom `WebViewClient` that hosts must set on the `WebView` |
| `PopupBridgeErrorListener` | Callback interface for error events |
| `PopupBridgeMessageListener` | Callback interface for successful popup completion |
| `PopupBridgeNavigationListener` | Callback interface for navigation events |

Internal implementation lives under `internal/` and `network/` sub-packages and is not part of the public API.

### Dependencies of note

- **browser-switch 3.0.0** — handles the actual browser launch and deep-link return
- **AndroidX DataStore Preferences** — `DeviceRepository` persists a device ID across app launches
- **Kotlin Coroutines** — async analytics dispatch

## Code quality

Detekt is configured with `maxIssues=0` (`detekt/detekt-config.yml`). All Detekt and Lint warnings are treated as errors. Run `./gradlew detekt --auto-correct` before pushing if you make formatting changes.

Max line length is **120 characters**. Java/Kotlin target compatibility is **Java 11** (library) / **Java 17** (demo).

## Publishing

- **Snapshot:** published automatically on every push to `main` (see `.github/workflows/release_snapshot.yml`).
- **Release:** triggered manually via GitHub Actions (`release.yml`) with a semver version input.
- Artifact coordinates: `com.braintreepayments.api:popup-bridge:<version>` on Maven Central / Sonatype.
- ProGuard rule in `PopupBridge/proguard.pro` keeps the public `PopupBridgeClient` class.
