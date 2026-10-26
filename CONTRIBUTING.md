# Contributing

1. Create a branch from `main`.
2. Keep the project free of private keys, credentials and proprietary assets.
3. Run `gradle testDebugUnitTest lintDebug assembleDebug` with Gradle 9.6.
4. Test launcher-role selection and app opening on a physical Android device.
5. Open a focused pull request and explain user-visible changes.

## Style

- Java 17 source with Android framework APIs only unless a dependency is clearly justified.
- Four-space indentation for Java and Gradle files.
- Keep the UI text-first, touch-accessible and usable without a hardware keyboard.
- Add parser unit tests for every new terminal command.

## Versioning

Use Semantic Versioning. Only maintainers push `vMAJOR.MINOR.PATCH` tags because every matching tag starts the signed release workflow.
