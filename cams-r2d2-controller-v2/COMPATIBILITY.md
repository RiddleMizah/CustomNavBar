# Cam V2 compatibility build

This branch restores the Android application ID `com.riddle.camsr2d2` so the disconnect-enabled build keeps the same app identity as the original Cam V2 release. It also restores Cam as the default in-app controller name while retaining Parent Mode name customization.

Because early CI builds used temporary signing keys, replacing one of those installs may require one final uninstall/reinstall. Published builds should use one retained release key so later APKs update normally with `adb install -r`.
