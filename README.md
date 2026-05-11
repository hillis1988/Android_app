# StarFleet Idle

A space-themed idle/incremental game for Android. Build your fleet, earn credits, and conquer the galaxy.

## Prerequisites

- **Android Studio** (Hedgehog 2023.1+ recommended) — [Download](https://developer.android.com/studio)
- OR **Android SDK Command Line Tools** if you prefer CLI-only

## Quick Start with Android Studio

1. Open this folder in Android Studio
2. Let Gradle sync complete
3. Click **Run** (green play button) — it will create an emulator automatically if needed
4. The app launches on the emulator

## CLI Build & Emulator

If you prefer command line:

```bash
# Build the debug APK
./gradlew assembleDebug

# List available AVDs
emulator -list-avds

# Create an AVD (if none exist)
sdkmanager "system-images;android-34;google_apis;x86_64"
avdmanager create avd -n Pixel6 -k "system-images;android-34;google_apis;x86_64" -d pixel_6

# Start the emulator
emulator -avd Pixel6

# Install and run
adb install app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.starfleet.idle/.MainActivity
```

## Game Guide

- **Earn credits** passively from your fleet
- **Buy ships** to increase income (6 tiers from Scout to Dreadnought)
- **Upgrade ships** with Engine and Hull improvements
- **Unlock tiers** by reaching Fleet Power milestones
- **Win condition**: 10 Dreadnoughts + 200K Fleet Power
- **Offline earnings**: Your fleet earns at 50% rate while you're away (up to 12 hours)

Designed for ~10 days of play to complete.
