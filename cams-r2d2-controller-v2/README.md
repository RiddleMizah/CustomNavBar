# R2-D2 Controller for Android

A kid-friendly Android controller for the 2016 Hasbro Smart R2-D2. It connects directly to the toy over Bluetooth Low Energy and does not require the discontinued Hasbro app, a Flipper Zero, a Raspberry Pi, or an internet connection.

The app includes driving controls, head movement, lights, sounds, preset dances, custom routine recording and playback, a visible Bluetooth disconnect button, and a protected Parent Mode with diagnostics and personalization.

> This is an unofficial fan project and is not affiliated with Hasbro, Disney, Lucasfilm, or Flipper Devices.

---

## For non-technical users

### What you need

- A compatible Hasbro Smart R2-D2 that advertises as `Kipps` or `2ndHeroD`
- An Android tablet or phone with Bluetooth Low Energy
- Android 8.0 or newer
- The release APK from this repository's GitHub Actions artifact or Releases page

### Install the app

1. Download `R2D2-Controller.apk` or `app-release.apk`.
2. Open the APK on the Android device.
3. Allow installation from the browser or file manager if Android asks.
4. Open **R2-D2 Controller**.
5. Allow the **Nearby devices** permission.
6. Turn on R2-D2 and tap **Connect R2-D2**.

The app has a separate **Disconnect** button in the top bar. The Home screen and Parent Mode also include a disconnect control. Disconnecting safely stops motion and any running routine before closing Bluetooth.

### Change the child's name

The default name is **Cam**, but it can be changed without rebuilding the app:

1. Press and hold **Parent Mode** in the lower-left corner.
2. Tap **Change Name**.
3. Enter any name up to 24 characters.
4. Tap **Save**.

The app immediately changes titles such as `Cam's R2-D2 Controller` to the new name. The launcher icon remains simply **R2-D2 Controller**, which makes the same APK easy to share with other families.

### Main screens

- **Home:** Connect or disconnect, then jump to the major features.
- **Drive:** Hold a direction to move; releasing it stops R2-D2.
- **Dances:** Play six included routines.
- **My Moves:** Record, save, rename, replay, and delete custom routines.
- **Parent Mode:** Change the name, connect or disconnect, view diagnostics, copy logs, open Bluetooth settings, and clear saved data.

### Safety

- Test movement on an open floor, never on a table or counter.
- Keep fingers, pets, and loose objects away from the wheels.
- Use **EMERGENCY STOP** if a movement or routine needs to stop immediately.
- Preset timing can behave slightly differently between toy revisions or floor surfaces.

---

## For technical users and developers

### Project requirements

- JDK 17
- Gradle 8.10 or newer
- Android SDK 35
- Android Build Tools 35.0.0

The project intentionally uses the Android framework UI rather than external UI libraries, keeping the APK small and the build simple.

### Build a debug APK

From the repository root:

```bash
gradle --no-daemon :app:assembleDebug
```

Output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

### Build the installable release APK

```bash
gradle --no-daemon :app:assembleRelease
```

Output:

```text
app/build/outputs/apk/release/app-release.apk
```

The included release configuration is development-signed so hobbyists can install it immediately. Anyone distributing the app publicly should replace that signing configuration with their own private release keystore and keep the keystore out of Git.

### Set a different default name at build time

Runtime personalization is available in Parent Mode. Developers can also change the initial default during a build:

```bash
gradle --no-daemon :app:assembleRelease -PcontrollerName="Alex"
```

That value is compiled into `BuildConfig.DEFAULT_CONTROLLER_NAME`. A name saved in Parent Mode takes precedence over the build-time default.

### ADB installation

```powershell
.\adb.exe install -r ".\app-release.apk"
```

Launch the generic package:

```powershell
.\adb.exe shell am start -n com.riddle.r2d2controller/com.riddle.camsr2d2.MainActivity
```

### Android configuration

- Namespace: `com.riddle.camsr2d2`
- Application ID: `com.riddle.r2d2controller`
- Minimum SDK: 26
- Target SDK: 35
- Orientation: landscape
- Internet permission: not requested
- Android 12+ permissions: `BLUETOOTH_SCAN` and `BLUETOOTH_CONNECT`
- Android 11 and older: location permission is requested only because older Android versions require it for BLE scanning

### BLE protocol

The working protocol is preserved from the original controller:

- Advertised names: `Kipps`, `2ndHeroD`
- Service UUID: `DAB91435-B5A1-E29C-B041-BCD562613BE4`
- Write characteristic: `DAB91383-B5A1-E29C-B041-BCD562613BE4`
- Notify characteristic: `DAB91382-B5A1-E29C-B041-BCD562613BE4`
- Keepalive: every 2 seconds
- Mechanical cam-settle delay: 400 ms before motor commands
- Write type: GATT write without response

### Architecture

- `R2D2Client` — scanning, connection, GATT discovery, writes, notifications, keepalive, and disconnect
- `Protocol` — UUIDs and command packet definitions
- `MotionController` — safe movement sequencing, cam delay, stop, and routine execution
- `RoutineRecorder` — records action timing and movement duration
- `RoutinePlayer` — plays timed steps and supports cancellation
- `RoutineStore` — stores custom routines locally as JSON
- `AppPreferences` — stores the personalized controller name
- `ParentScreen` — diagnostics, logs, personalization, Bluetooth tools, and reset actions

### GitHub Actions

The included workflow installs Android SDK 35, builds debug and release APKs, uploads both APKs, and creates a standalone source package suitable for uploading into a new repository.

### Sharing or forking

The launcher name and package are generic. A fork usually only needs to change:

- The build-time default name with `-PcontrollerName=...`
- The app icon, if desired
- `applicationId`, if publishing a separate Play Store listing
- Release signing configuration

Do not commit private signing keys, passwords, or generated local Android SDK files.

---

## Included features

- Direct BLE connection to R2-D2
- Connect, disconnect, and reconnect controls
- Hold-to-drive with automatic stop on release
- Emergency stop
- Head left, center, and right
- Red, blue, and off light controls
- Wake, whistle, and achievement sounds
- Six preset routines
- Custom timed routine recording and playback
- Local saved routines
- Runtime controller-name personalization
- Parent Mode diagnostics and event log
- Copyable technical report
- No account, analytics, advertisements, or internet dependency

## Known limitations

- Tested protocol support is limited to toys advertising as `Kipps` or `2ndHeroD`.
- This project does not include official Hasbro assets or firmware.
- Preset dance timing should be tested on an open floor with each toy revision.
- The app is optimized for landscape tablets, though compatible phones may also run it.
