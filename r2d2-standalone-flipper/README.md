# Standalone Hasbro Smart R2-D2 controller for Flipper Zero

This project builds a custom Flipper Zero firmware package that controls the 2016 Hasbro Smart R2-D2 directly over Bluetooth Low Energy. No phone, tablet, Raspberry Pi, or external Bluetooth module is used.

## Why a firmware package is required

R2-D2 is a BLE GATT peripheral. The Flipper must operate as a BLE central, scan for the toy, connect, discover its command characteristic, and issue GATT writes. The STM32WB radio and full Bluetooth stack support this, but the stock firmware does not export the central-mode ACI calls to ordinary `.fap` applications.

The build therefore:

1. Uses the official Flipper firmware 1.4.3 base.
2. Installs ST's BLE Full coprocessor stack.
3. Enables the central role alongside the normal peripheral role.
4. Compiles the R2-D2 controller into the firmware as a menu application.

## R2-D2 protocol

The toy advertises as `Kipps` or `2ndHeroD` and exposes:

- Service: `DAB91435-B5A1-E29C-B041-BCD562613BE4`
- Write characteristic: `DAB91383-B5A1-E29C-B041-BCD562613BE4`

The app automatically scans, connects, discovers the handles, and sends a keepalive every two seconds.

## Controls

### Drive mode

- Up: forward
- Down: reverse
- Left/right: pivot
- Release a direction: stop
- OK: emergency stop

### Head / Sound mode

- Left/right: head left/right
- OK: center head
- Up: wake sound
- Down: whistle

### Lights mode

- Left: red
- Right: blue
- Down or OK: off
- Up: achievement sound

Hold OK to cycle modes. Press Back to stop and exit.

## Install

Download the generated updater `.tgz`, connect the Flipper to qFlipper, and choose **Install from file**. This replaces the currently installed firmware with the official-based R2-D2 build. Back up the Flipper first if you use custom firmware or have settings you care about.

After updating, power on R2-D2 and open **R2-D2** from the Flipper menu.

## Safety

Test with R2-D2 on an open floor. Movement commands stop on direction release, but this has not been physically validated against every toy revision.
