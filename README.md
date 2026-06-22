<!-- AETHERDESK - Mobile Header -->
<div align="center">
    <img src="app/src/main/assets/www/images/logo_transparent.png" width="200" alt="AetherDesk Mobile Logo">
  
  # AetherDesk Mobile
  
  **A 100% offline, local-first AI workspace and productivity suite.**
  
  <!-- Technical Stack Badges -->
  <img src="https://img.shields.io/badge/Android-15%2B-3DDC84?style=plastic&logo=android">
  <img src="https://img.shields.io/badge/Language-Kotlin-blue?style=plastic&logo=kotlin">
  <img src="https://img.shields.io/badge/Frontend-HTML5%2FCSS3-E34F26?style=plastic&logo=html5">
  <img src="https://img.shields.io/badge/Engine-WebView_Hybrid-9C27B0?style=plastic&logo=googlechrome">
  
  <br>
  
  <!-- Project Status & Legal Badges -->
  <img src="https://img.shields.io/badge/Status-Stable_v1.0.0-success?style=plastic">
  <img src="https://img.shields.io/badge/Scanner-ML_Kit_Full-4285F4?style=plastic&logo=google">
  <img src="https://img.shields.io/badge/License-All_Rights_Reserved-red?style=plastic">
</div>

<br>

**AetherDesk Mobile** is a powerful, hybrid Android application that delivers a premium document scanning and management experience without ever compromising your privacy. Designed from the ground up to be completely offline and lightning-fast, it bridges the gap between fluid web interfaces and raw native device power.

Wrapped in our breathtaking **Liquid Glass Frost** design system, AetherDesk effortlessly adapts to both Light and Dark modes, proving that utility tools can be both highly functional and visually stunning.

---

## ✨ Core Features

### 📸 High-Fidelity Scanning Engine
* **Hardware-Accelerated:** Leverages the native [Google ML Kit Document Scanner API](https://developers.google.com/ml-kit/vision/doc-scanner) for instant edge detection, automatic cropping, and shadow filtering.
* **Intelligent Memory Management:** Built-in safeguards, including a strict 38-page batch limit, guarantee smooth, crash-free operation on all Android devices by preventing `OutOfMemoryError` (OOM) states.

### ⚡ Native Android Integrations
* **System-Level Handoffs:** Seamlessly exports files to PDF or High-Res Images directly to native system intents (WhatsApp, Google Drive, OneDrive).
* **Exact Sizing Metrics:** Bypasses inaccurate estimations by using in-memory Blobs and native `PdfDocument` serialization to calculate exact byte sizes before export.
* **Smart Notifications:** Real-time, interactive native Android system notifications upon successful export (fully compliant with Android 13+ API 33 `POST_NOTIFICATIONS`).

### 🎨 Premium UI/UX
* **Liquid Glass Frost Theme:** A bespoke, responsive hybrid web-layer featuring deep backdrop blurs, dynamic variable styling, and seamless Light/Dark mode parity.
* **Fluid Mechanics:** Lightning-fast, hardware-accelerated UI with buttery-smooth `SortableJS` drag-and-drop page reorganization.

---

## 🏗️ Architecture Under the Hood
AetherDesk utilizes a dual-brain hybrid architecture:
1. **The Native Bridge (`Kotlin`):** Handles heavy lifting, hardware telemetry, native intents, system notifications, and ML Kit integrations.
2. **The Visual Layer (`HTML5/CSS/JS`):** Renders the Liquid Glass Frost UI, handles offline IndexedDB routing, and manages the interactive workspace.

---

## 🚀 Future Roadmap
* **Android 15+ Optimization:** Transition the target SDK to Android 15 to leverage the latest system-level privacy and performance APIs.
* **Universal File Viewer:** Full implementation of the native lens system for document and code previews.
* **Recents Dashboard:** Integration of the AetherDB IndexedDB engine to surface recent scans directly on the home screen.

---

## 📚 Documentation
Ready to explore the codebase or set up your own local environment? Refer to our dedicated documentation files:

| File | Description |
| :--- | :--- |
| 🚀 **[Setup Instructions](SETUP.md)** | Guide to configuring Android Studio, Gradle, and local dependencies. |
| 📖 **[User Guide](GUIDE.md)** | Comprehensive manual on utilizing AetherDesk's offline features. |
| 🤝 **[Contributing](CONTRIBUTING.md)** | Guidelines for branching, commits, and pull requests. |
| 🛡️ **[Code of Conduct](CODE_OF_CONDUCT.md)** | Rules and expectations for community interaction. |

---

## ⚖️ License & Copyright

**Copyright © 2026 Rishit Ghosh. All rights reserved.**

This software is provided for personal use and portfolio demonstration. **Any use, modification, reproduction, or distribution of this code requires explicit written permission from the author.** Please refer to the [Code of Conduct](CODE_OF_CONDUCT.md) for more details.

<div align="center">
  <br>
  <i>Built with ❤️ by <a href="https://github.com/rajghosh06-dev">Rishit Ghosh</a> and Antigravity.</i>
</div>