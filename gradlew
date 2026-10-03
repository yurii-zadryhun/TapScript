#!/usr/bin/env bash
set -euo pipefail

# Small bootstrap wrapper. It intentionally avoids committing a binary wrapper JAR.
# Android Studio can import the project directly; CLI users get Gradle 9.6.0 automatically.
GRADLE_VERSION="9.6.0"
GRADLE_SHA256="bbaeb2fef8710818cf0e261201dab964c572f92b942812df0c3620d62a529a01"
CACHE_DIR="${GRADLE_USER_HOME:-$HOME/.gradle}/tapscript-bootstrap/gradle-${GRADLE_VERSION}"
GRADLE_BIN="$CACHE_DIR/bin/gradle"

if [[ ! -x "$GRADLE_BIN" ]]; then
  TMP_DIR="$(mktemp -d)"
  trap 'rm -rf "$TMP_DIR"' EXIT
  ARCHIVE="$TMP_DIR/gradle.zip"
  URL="https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"
  echo "Downloading Gradle ${GRADLE_VERSION}..." >&2
  if command -v curl >/dev/null 2>&1; then
    curl -fL "$URL" -o "$ARCHIVE"
  elif command -v wget >/dev/null 2>&1; then
    wget -O "$ARCHIVE" "$URL"
  else
    echo "curl or wget is required for the first CLI build." >&2
    exit 1
  fi
  if command -v sha256sum >/dev/null 2>&1; then
    echo "$GRADLE_SHA256  $ARCHIVE" | sha256sum --check --status || {
      echo "Gradle archive checksum verification failed." >&2
      exit 1
    }
  fi
  mkdir -p "$(dirname "$CACHE_DIR")"
  unzip -q "$ARCHIVE" -d "$(dirname "$CACHE_DIR")"
fi

exec "$GRADLE_BIN" "$@"
