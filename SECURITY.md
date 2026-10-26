# Security policy

## Supported versions

Security fixes are provided for the latest published version.

## Reporting a vulnerability

Open a private security advisory from the repository **Security** tab. Do not open a public issue containing exploit details, private keys or credentials.

## Signing key handling

- Never commit the release keystore, its Base64 representation or passwords.
- Store the keystore in an encrypted offline backup.
- Store CI credentials only as GitHub Actions secrets.
- Rotate exposed credentials immediately. Remember that changing the app-signing key can prevent existing installations from receiving updates.

DOSDeck does not request network, contacts, location, storage or notification permissions. It reads only activities exposed as launchable applications by Android.
