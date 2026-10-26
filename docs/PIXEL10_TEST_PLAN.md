# Pixel 10 acceptance test

Use a Google Pixel 10 with the current production Android update.

1. Install the APK: `adb install DOSDeck-Launcher-<version>.apk`.
2. Open DOSDeck and approve the Android request to make it the Home app.
3. Press the physical or gesture Home action. DOSDeck must reopen without a crash.
4. Run `DIR`; verify that launchable applications appear and can be opened by touch.
5. Run `START 1`, `FIND camera`, `SETTINGS`, `WALLPAPER`, `DATE`, `TIME` and `VER`.
6. Test gesture navigation, three-button navigation, portrait, landscape, dark mode and a large system font.
7. Install an application, return to DOSDeck and run `RELOAD`; the application must appear.
8. Run `COLOR AMBER` and `FONT LARGE`, restart the launcher and verify that both preferences persist.
9. Run `SYSTEM` and verify that Android opens the default Home-app settings.
10. Upgrade over the previous signed version with `adb install -r`; Android must accept the update.

Pass criteria: no clipped controls, no touch target hidden by the display cutout or system bars, no crash, successful app launches and a valid upgrade path.
