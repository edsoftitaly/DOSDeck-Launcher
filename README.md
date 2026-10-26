# DOSDeck Launcher

DOSDeck is a real Android Home app with a text-first interface inspired by classic MS-DOS terminals. It replaces a conventional icon grid with a command prompt, an application directory and touch-enabled program names.

The project is ready for GitHub: it includes the Android source, tests, reproducible Gradle setup in GitHub Actions, release signing, checksum generation and automatic publication of the final APK in GitHub Releases.

> DOSDeck is an independent project. It is not affiliated with or endorsed by Microsoft or Google. MS-DOS is a Microsoft trademark.

## Main features

- Qualifies as an Android Home app and requests the system `HOME` role.
- Lists only launchable applications exposed by Android.
- Opens apps by touch, directory number, name or package search.
- Provides `DIR`, `START`, `FIND`, `RELOAD`, `HOME`, `SETTINGS`, `WALLPAPER`, `SYSTEM`, `COLOR`, `FONT`, `DATE`, `TIME`, `VER`, `ABOUT` and `CLS` commands.
- Recreates a monochrome DOS-style terminal with square controls, monospaced text, boot output and a subtle CRT scanline overlay.
- Stores only the selected terminal color, font size and whether the initial Home-role prompt was shown.
- Requests no network, location, contacts, storage or notification permission.
- Targets Android API 37 and uses no native libraries, so one universal APK runs on the Google Pixel 10 and other ARM64 Android phones.
- Requires Android 10/API 29 or newer.

## Project layout

```text
.
├── app/                              Android application
├── .github/workflows/ci.yml          tests, lint and debug APK
├── .github/workflows/release.yml     signed APK and GitHub Release
├── docs/PIXEL10_TEST_PLAN.md         physical-device acceptance test
├── scripts/generate-keystore.sh      local signing-key helper
├── scripts/setup-android-sdk.sh      CI-safe Android SDK setup
├── scripts/publish-release.sh        validated semantic tag publisher
├── build.gradle / settings.gradle     Reproducible Android build
├── SECURITY.md
└── CHANGELOG.md
```

## Fastest path: publish to GitHub

Prerequisites: Git, GitHub CLI (`gh`) and an authenticated GitHub account. GitHub Actions supplies Java 17, Gradle 9.6 and the Android SDK.

```bash
unzip DOSDeck-Launcher.zip
cd DOSDeck-Launcher
chmod +x scripts/*.sh

git init
git add .
git commit -m "Initial DOSDeck Launcher release"
git branch -M main

gh auth login
gh repo create dosdeck-launcher --public --source=. --remote=origin --push
```

The first push runs the CI workflow and creates a downloadable debug APK as a workflow artifact. The workflow locates `sdkmanager` from the runner's Android SDK instead of assuming it is already on `PATH`, accepts licenses without hiding command failures, and verifies the required SDK files before building. A production GitHub Release requires the four signing secrets below.

## Configure release signing once

Android requires APKs to be signed. The private key must never be committed.

Generate a long-lived local keystore:

```bash
./scripts/generate-keystore.sh dosdeck-release.jks dosdeck
```

Keep `dosdeck-release.jks` and its password in an encrypted offline backup. Then configure the repository secrets:

```bash
gh secret set SIGNING_KEYSTORE_BASE64 < dosdeck-release.jks.base64
gh secret set SIGNING_STORE_PASSWORD
gh secret set SIGNING_KEY_ALIAS --body "dosdeck"
gh secret set SIGNING_KEY_PASSWORD
```

`SIGNING_STORE_PASSWORD` and `SIGNING_KEY_PASSWORD` must match the password entered in the helper script. The workflow reconstructs the keystore only inside the temporary GitHub runner and removes it when the runner is destroyed.

## Publish the first final APK

```bash
./scripts/publish-release.sh v1.0.0
```

Pushing a tag matching `vMAJOR.MINOR.PATCH` automatically:

1. checks out the exact tag;
2. installs Gradle 9.6 and Android SDK 37;
3. runs unit tests and release lint;
4. creates a minified, resource-shrunk, signed release APK;
5. verifies the APK signature with `apksigner`;
6. creates a SHA-256 checksum;
7. creates the GitHub Release with generated release notes;
8. attaches `DOSDeck-Launcher-1.0.0.apk` and its `.sha256` file.

The release workflow fails instead of publishing an unsigned APK when any signing secret is missing.

## Local development

Use Java 17, Gradle 9.6 and Android SDK Platform 37. Create `local.properties` with your Android SDK path if Android Studio has not already done so.

Linux/macOS:

```properties
sdk.dir=/home/your-user/Android/Sdk
```

Build and verify:

```bash
gradle clean testDebugUnitTest lintDebug assembleDebug
```

Install the debug build:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The debug variant uses the package name `com.edoardonapoli.dosdeck.debug`, so it can coexist with the signed production build.

## Use DOSDeck

1. Install and open DOSDeck.
2. Accept the Android system prompt to use it as the Home app. You can repeat this with `HOME` or `[SET DEFAULT]`.
3. Type `DIR` or tap `[DIR]`.
4. Tap an underlined application name, or run `START 12` or `START camera`.
5. Run `SYSTEM` to open Android's default Home-app settings and switch launcher.

## Pixel 10 validation

The app uses only stable Android framework APIs, supports display cutouts and system bars, and contains no architecture-specific native code. Before publishing broadly, execute the physical-device checklist in [docs/PIXEL10_TEST_PLAN.md](docs/PIXEL10_TEST_PLAN.md) on the exact Pixel 10 and Android build you intend to support.

## Release recovery

If the release job fails:

- open **Actions → Publish signed APK** and read the failed step;
- correct the repository secret or code;
- push a new patch tag, for example `v1.0.1`;
- do not reuse a published version number for a different binary.

If the signing key is lost, existing installations cannot accept updates signed with a different key. Restore the encrypted backup rather than generating a replacement.

## Official technical references

- Android SDK platform releases: https://developer.android.com/tools/releases/platforms
- Android `RoleManager`: https://developer.android.com/reference/android/app/role/RoleManager
- Android app signing: https://developer.android.com/studio/publish/app-signing
- GitHub Actions authentication: https://docs.github.com/actions/security-guides/automatic-token-authentication
- GitHub CLI in workflows: https://docs.github.com/actions/using-workflows/using-github-cli-in-workflows

## License

MIT. See [LICENSE](LICENSE).
