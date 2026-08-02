# Updating the Cam V2 app

The compatibility build uses package `com.riddle.camsr2d2`.

Early APKs were signed by temporary CI keys. If Android reports `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, uninstall the old Cam V2 package once, install the retained-key release, and use `adb install -r` for later retained-key releases.
