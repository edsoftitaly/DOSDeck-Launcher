# Changelog

All notable changes are documented here. Versioning follows Semantic Versioning.

## [Unreleased]

### Fixed

- GitHub Actions now resolves `sdkmanager` from the Android SDK installation instead of assuming it is on `PATH`.
- Android license acceptance no longer hides genuine `sdkmanager` failures or reports the harmless `yes` SIGPIPE as a failed job.
- CI verifies Platform 37, Build Tools 36.0.0 and Platform Tools before starting Gradle.

## [1.0.0] - 2026-10-26

### Added

- Text-first Android launcher with a classic DOS terminal look.
- Touch-enabled application directory and command parser.
- Android Home role request and launcher intent filters.
- Color and font preferences.
- Android API 37 target with Pixel-class edge-to-edge compatibility.
- Continuous integration for tests, lint and debug APK builds.
- Signed APK publication to GitHub Releases when a semantic-version tag is pushed.
- SHA-256 checksum and APK signature verification in the release workflow.
