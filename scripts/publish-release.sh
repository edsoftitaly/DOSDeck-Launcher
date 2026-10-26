#!/usr/bin/env bash
set -euo pipefail

TAG="${1:-}"
[[ "$TAG" =~ ^v[0-9]+\.[0-9]+\.[0-9]+$ ]] || {
  echo "Usage: $0 vMAJOR.MINOR.PATCH" >&2
  exit 1
}

git diff --quiet && git diff --cached --quiet || {
  echo "Working tree has uncommitted changes." >&2
  exit 1
}

git rev-parse --verify "$TAG" >/dev/null 2>&1 && {
  echo "Tag already exists locally: $TAG" >&2
  exit 1
}

git tag -a "$TAG" -m "DOSDeck Launcher $TAG"
git push origin "$TAG"
echo "Tag pushed. GitHub Actions will build, sign and publish the APK automatically."
