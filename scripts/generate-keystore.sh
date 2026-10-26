#!/usr/bin/env bash
set -euo pipefail

KEYSTORE="${1:-dosdeck-release.jks}"
ALIAS="${2:-dosdeck}"

if [[ -e "$KEYSTORE" ]]; then
  echo "Refusing to overwrite existing file: $KEYSTORE" >&2
  exit 1
fi

read -r -s -p "Keystore/key password: " PASSWORD
echo
[[ ${#PASSWORD} -ge 12 ]] || { echo "Use a password of at least 12 characters." >&2; exit 1; }

keytool -genkeypair \
  -v \
  -keystore "$KEYSTORE" \
  -storepass "$PASSWORD" \
  -keypass "$PASSWORD" \
  -alias "$ALIAS" \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000 \
  -dname "CN=DOSDeck Launcher, OU=Mobile, O=Edoardo Napoli, L=Genova, C=IT"

base64 < "$KEYSTORE" | tr -d '\n' > "${KEYSTORE}.base64"
chmod 600 "$KEYSTORE" "${KEYSTORE}.base64"

cat <<EOF
Created:
  $KEYSTORE
  ${KEYSTORE}.base64

Configure these GitHub secrets:
  SIGNING_KEYSTORE_BASE64  = contents of ${KEYSTORE}.base64
  SIGNING_STORE_PASSWORD   = the password you entered
  SIGNING_KEY_ALIAS        = $ALIAS
  SIGNING_KEY_PASSWORD     = the same password

Keep the JKS and password in a secure offline backup. Never commit them.
EOF
