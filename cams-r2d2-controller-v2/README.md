# Cam's R2-D2 Controller V2

A polished Android tablet controller for the 2016 Hasbro Smart R2-D2, built for Cam's Samsung Galaxy Tab A9+.

## What V2 adds

- Kid-friendly blue, white, and yellow interface.
- Home, Drive, Dances, and My Moves screens.
- Six built-in routines.
- Record touch actions and motion timing, save them, rename them, replay them, or delete them.
- Hold-to-drive controls with automatic stop on release.
- Dedicated emergency stop.
- Long-press Parent Mode with device details, Bluetooth permission state, BLE UUIDs, connection diagnostics, technical log, reconnect tools, and reset controls.
- No internet, account, camera, microphone, storage, contacts, or location permission on Android 12+.

## R2-D2 protocol

The app keeps the exact working protocol from V1:

- Advertised names: `Kipps`, `2ndHeroD`
- Service: `DAB91435-B5A1-E29C-B041-BCD562613BE4`
- Write: `DAB91383-B5A1-E29C-B041-BCD562613BE4`
- Notify: `DAB91382-B5A1-E29C-B041-BCD562613BE4`
- Keepalive every two seconds
- 400 ms cam-settle delay before motor commands

## Build

Requires JDK 17, Gradle 8.10+, and Android SDK 35.

```bash
gradle --no-daemon :app:assembleDebug
```

APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Install beside V1

V2 uses package `com.riddle.camsr2d2`, so it can be installed alongside the original debug controller without replacing it.

## Safety

Test movement routines on an open floor. Do not place R2-D2 on a table while using drive commands.

This is an unofficial fan project and is not affiliated with Hasbro, Disney, Lucasfilm, or Flipper Devices.
