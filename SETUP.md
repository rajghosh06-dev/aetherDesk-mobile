# Setup & Installation

This document provides instructions on how to set up the AetherDesk Mobile development environment.

## Prerequisites
- [Android Studio](https://developer.android.com/studio) (Latest stable version recommended)
- Java Development Kit (JDK 17 or higher)
- Android SDK API level 33 or higher (Android 13+)
- Git

## Installation Steps
1. **Clone the Repository**
   ```bash
   git clone https://github.com/rajghosh06-dev/aetherDesk-mobile.git
   cd aetherDesk-mobile
   ```
   *(Note: The repository strictly protects brand SVGs and necessary UI assets so everything works right out of the box).*

2. **Open the Project**
   - Launch Android Studio.
   - Click on **Open**, and navigate to the directory where you cloned the project.
   - Let Gradle sync completely.

3. **Connect a Device**
   - Connect your Android device via USB or start an Android Virtual Device (AVD).
   - Ensure Developer Options and USB Debugging are enabled on your physical device.

4. **Build and Run**
   - Click the **Run 'app'** (green play button) in Android Studio.
   - The app will compile, install onto the device, and launch automatically.

## Troubleshooting
- If you face caching or Gradle issues, try clicking **File > Invalidate Caches...** and then **Build > Clean Project**.
- Ensure that you have the correct Android SDK build tools installed as defined in `app/build.gradle.kts`.

## Permissions Notice
When running the application for the first time, it will prompt for the `POST_NOTIFICATIONS` permission (on Android 13+) and media/camera permissions. These are essential for the document scanner and the native file export notifications to function correctly.

---
*Built by Raj Ghosh and Antigravity.*
