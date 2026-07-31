# R2-D2 Flipper Controller

Android tablet controller for the 2016 Hasbro Smart R2-D2 (B7493), with hardware-key mappings designed for a paired Flipper Zero Bluetooth HID remote.

## Architecture

The Flipper Zero is used as the physical Bluetooth remote. The Android tablet receives its HID key presses, then this app sends the corresponding Bluetooth LE commands to R2-D2.

```text
Flipper Zero --Bluetooth HID--> Android tablet --Bluetooth LE--> Hasbro R2-D2
```

This avoids depending on unsupported direct BLE-central behavior in a normal Flipper application while still making the Flipper the handheld controller.

## Features

- Scans for the toy names `Kipps` and `2ndHeroD`.
- Connects over BLE using the reverse-engineered Hasbro service and characteristics.
- Touch controls for driving, pivot turns, head movement, lights, and sounds.
- Hardware keyboard/HID mappings for a paired Flipper Zero.
- Automatically sends STOP when a direction key is released.
- Cancels delayed motor starts if a key is released before the mechanical cam finishes changing position.
- Sends the toy keepalive packet every two seconds.

## Flipper and tablet setup

1. Leave Samsung Kids and install the APK from the tablet's parent profile.
2. On the Flipper, open **Apps → Bluetooth → Remote** and select a mode that sends arrow, page, media, or volume keys.
3. Pair the Flipper with the tablet in Android Bluetooth settings.
4. Open this app. The **Last Flipper key** field shows the Android key code sent by the selected remote mode.
5. Switch on R2-D2 and tap **Connect R2-D2**.

The app understands arrows, WASD, numpad directions, page up/down, media rewind/fast-forward, Enter/OK/Space, play-pause, volume keys, and several letter/number shortcuts.

## Build locally

Requires JDK 17, Android SDK 34, and Gradle 8.7:

```bash
gradle --no-daemon :app:assembleDebug
```

APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Safety

Test with R2-D2 on the floor, not on a table. This project is unofficial and is not affiliated with Hasbro, Disney, Lucasfilm, or Flipper Devices.
