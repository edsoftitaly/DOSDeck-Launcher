#!/usr/bin/env bash
set -euo pipefail

ANDROID_API_LEVEL="${ANDROID_API_LEVEL:-37}"
ANDROID_BUILD_TOOLS_VERSION="${ANDROID_BUILD_TOOLS_VERSION:-36.0.0}"

sdk_root="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-/usr/local/lib/android/sdk}}"
sdkmanager=""

# GitHub-hosted Ubuntu runners install sdkmanager here, but do not always expose
# it on PATH. Keep additional fallbacks so the script also works locally.
candidates=(
  "$sdk_root/cmdline-tools/latest/bin/sdkmanager"
  "$sdk_root/cmdline-tools/bin/sdkmanager"
  "$sdk_root/tools/bin/sdkmanager"
)

if command -v sdkmanager >/dev/null 2>&1; then
  candidates+=("$(command -v sdkmanager)")
fi

while IFS= read -r candidate; do
  candidates+=("$candidate")
done < <(find "$sdk_root/cmdline-tools" -type f -path '*/bin/sdkmanager' -print 2>/dev/null | sort -Vr)

for candidate in "${candidates[@]}"; do
  if [[ -x "$candidate" ]]; then
    sdkmanager="$candidate"
    break
  fi
done

if [[ -z "$sdkmanager" ]]; then
  echo "ERROR: sdkmanager was not found under Android SDK root: $sdk_root" >&2
  echo "Expected location: $sdk_root/cmdline-tools/latest/bin/sdkmanager" >&2
  exit 127
fi

export ANDROID_SDK_ROOT="$sdk_root"
export ANDROID_HOME="$sdk_root"

if [[ -n "${GITHUB_ENV:-}" ]]; then
  {
    echo "ANDROID_SDK_ROOT=$sdk_root"
    echo "ANDROID_HOME=$sdk_root"
  } >> "$GITHUB_ENV"
fi

if [[ -n "${GITHUB_PATH:-}" ]]; then
  {
    dirname "$sdkmanager"
    echo "$sdk_root/platform-tools"
    echo "$sdk_root/build-tools/$ANDROID_BUILD_TOOLS_VERSION"
  } >> "$GITHUB_PATH"
fi

echo "Using sdkmanager: $sdkmanager"
echo "Using Android SDK root: $sdk_root"
"$sdkmanager" --sdk_root="$sdk_root" --version

# GitHub runs bash with pipefail. sdkmanager can close stdin after the final
# answer, causing `yes` to exit with SIGPIPE even though license acceptance
# succeeded. Capture sdkmanager's status specifically instead of masking errors.
set +e
set +o pipefail
yes 2>/dev/null | "$sdkmanager" --sdk_root="$sdk_root" --licenses >/dev/null
license_status=${PIPESTATUS[1]}
set -o pipefail
set -e

if (( license_status != 0 )); then
  echo "ERROR: Android SDK license acceptance failed with exit code $license_status." >&2
  exit "$license_status"
fi

"$sdkmanager" --sdk_root="$sdk_root" --install \
  "platforms;android-$ANDROID_API_LEVEL" \
  "build-tools;$ANDROID_BUILD_TOOLS_VERSION" \
  "platform-tools"

required_paths=(
  "$sdk_root/platforms/android-$ANDROID_API_LEVEL/android.jar"
  "$sdk_root/build-tools/$ANDROID_BUILD_TOOLS_VERSION/apksigner"
  "$sdk_root/platform-tools/adb"
)

for required_path in "${required_paths[@]}"; do
  if [[ ! -e "$required_path" ]]; then
    echo "ERROR: Required Android SDK component was not installed: $required_path" >&2
    exit 1
  fi
done

echo "Android SDK setup completed successfully."
