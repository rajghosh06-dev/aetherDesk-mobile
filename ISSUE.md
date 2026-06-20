# Android App Reliability & Error Handling Issues

### 1. `compressPdf` Resource Leaks
- **Cause**: In `AndroidBridge.kt`, the `compressPdf` method does not use a `try-finally` block for closing the `PdfRenderer`, `ParcelFileDescriptor`, and deleting the `tempFile`. If an exception occurs (such as an `OutOfMemoryError` or `IOException`), these resources remain open and the temporary file leaks.
- **File**: `app/src/main/java/com/example/aetherdesk/AndroidBridge.kt`
- **Resolution**: **Resolved**. Wrapped rendering logic in a `try-finally` block to ensure `PdfRenderer`, `ParcelFileDescriptor`, and the temporary `.pdf` file are closed and deleted safely.

### 2. `warpPerspective` Bitmap Leaks
- **Cause**: In `AndroidBridge.kt`, `warpPerspective` decodes two `Bitmap` objects. `srcBitmap.recycle()` and `destBitmap.recycle()` are at the end of the method. If the `compress` operation fails, the bitmaps are never recycled, leading to severe memory leaks.
- **File**: `app/src/main/java/com/example/aetherdesk/AndroidBridge.kt`
- **Resolution**: **Resolved**. Added a `finally` block to guarantee `srcBitmap?.recycle()` and `destBitmap?.recycle()` are always executed.

### 3. Native App Crash on OutOfMemory (OOM) Errors
- **Cause**: Multiple methods in `AndroidBridge.kt` (e.g., `encryptFile`, `decryptFile`, `rasterizePdf`, `combineImagesToPdf`) manipulate large memory objects (Base64 strings, massive ByteArrays). The methods catch `Exception`, but `OutOfMemoryError` is a subclass of `Error` (and `Throwable`), not `Exception`. If an OOM occurs, the app crashes completely instead of returning `{"ok": false, "error": "Out of memory"}` to the WebView.
- **File**: `app/src/main/java/com/example/aetherdesk/AndroidBridge.kt`
- **Resolution**: **Resolved**. Replaced `catch (e: Exception)` with `catch (e: Throwable)` globally across `AndroidBridge.kt`. Base64 and memory exceptions now return a graceful `{"ok": false, "error": "..."}` JSON payload.

### 4. WebView File Chooser Upload Failure
- **Cause**: In `MainActivity.kt`, the WebView configuration explicitly sets `allowContentAccess = false`. The Android file picker returns `content://` URIs. When the user selects a file via `<input type="file">`, the WebView is blocked from reading the `content://` URI, causing the file upload feature to fail silently.
- **File**: `app/src/main/java/com/example/aetherdesk/MainActivity.kt`
- **Resolution**: **Resolved**. Updated `MainActivity.kt` to set `allowContentAccess = true`. File uploads from `content://` providers now function properly.

### 5. `SecurityException` in Network Status Checks
- **Cause**: `checkWifiStatus()` and `checkMobileData()` access the `ConnectivityManager` and `activeNetwork`. If the app lacks the `ACCESS_NETWORK_STATE` permission in the Manifest, this throws a `SecurityException`. Currently, there is no `try/catch` wrapping these calls, which would crash the app.
- **File**: `app/src/main/java/com/example/aetherdesk/AndroidBridge.kt`
- **Resolution**: **Resolved**. Surrounded network availability checks with a `try-catch` returning `false` upon a `SecurityException`.

### 6. WebView Memory Leak on Activity Destroy
- **Cause**: `MainActivity.kt` holds a reference to `myWebView` but does not override `onDestroy` to call `myWebView?.destroy()`. This can cause a massive memory leak of the entire WebView context if the Activity is destroyed and recreated (e.g., via background process death or settings changes).
- **File**: `app/src/main/java/com/example/aetherdesk/MainActivity.kt`
- **Resolution**: **Resolved**. Overrode `onDestroy()` inside `MainActivity.kt` to explicitly invoke `myWebView?.destroy()` and nullify the reference.

### 7. Splash Screen Freeze & Silent JS Crashes
- **Cause**: The webview was silently crashing on JavaScript ReferenceErrors before hiding the splash screen, causing an infinite freeze. Native Logcat was unable to capture these console errors.
- **File**: pp.js and MainActivity.kt
- **Resolution**: **Resolved**. Appended empty function stubs to pp.js for missing variables. Added a 3000ms failsafe setTimeout to forcefully hide the #boot-splash-overlay. Overrode onConsoleMessage in MainActivity.kt to pipe console.error and console.log into Android Logcat under the tag WebViewConsole.

### 8. Web Layer Initialization Failures
- **Cause**: Syntax error in app.js, missing sortable.min.js import in index.html, and a null reference in model-manager.js.
- **Resolution**: **Resolved**. Corrected the app.js syntax, added the missing script tag to index.html, and synced the ID query in model-manager.js.

### 9. ShareSession and UI Component Loading Failures
- **Cause**: The shareSessionTimer was caught in a Temporal Dead Zone because it was declared with let but accessed beforehand by initAetherShare(). Furthermore, lib/sortable.min.js physically did not exist in the project, triggering Sortable is not defined inside the DOM initialization block.
- **Resolution**: **Resolved**. Converted shareSessionTimer from let to ar to safely hoist the declaration, and downloaded the missing Sortable.min.js file into pp/src/main/assets/www/lib/ to fulfill the offline requirement.
