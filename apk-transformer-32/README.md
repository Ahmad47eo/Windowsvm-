# APK Transformer 32

An Android TV-friendly APK compatibility inspector. It checks native library ABIs inside an APK and explains whether a 32-bit ARM device has the native libraries it needs.

**Important:** this tool does not magically translate 64-bit native code into 32-bit code. Real conversion requires source code or compatible 32-bit native libraries and a rebuild. The app analyzes APK contents; it does not modify or re-sign third-party apps.

## Build

The repository's GitHub Actions workflow builds a debug APK for testing. Open **Actions → APK Transformer 32 Android Build** and download the `apk-transformer-32-debug-apk` artifact after a successful run.

## Features

- Android document picker for selecting an APK
- Detects `armeabi-v7a`, `arm64-v8a`, x86 and x86_64 native libraries
- Explains ABI compatibility and limitations
- Designed with large touch targets for Android TV remotes

The first version is an analyzer, not a binary translator.