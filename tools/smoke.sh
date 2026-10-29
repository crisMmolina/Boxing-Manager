#!/usr/bin/env bash
# Smoke del producto (lo usa .uak/bin/smoke-project en cada merge).
# core siempre; Android si hay SDK en esta máquina; la central si hay JDK 25 o es Windows.
set -eu
cd "$(dirname "$0")/.."

echo "== core"
core/gradlew -p core build -q

if [ -z "${ANDROID_HOME:-}" ] && [ -d "$HOME/Library/Android/sdk" ]; then export ANDROID_HOME="$HOME/Library/Android/sdk"; fi
if [ -n "${ANDROID_HOME:-}" ] && [ -f android/app/build.gradle.kts ]; then
  echo "== android"
  android/gradlew -p android :app:assembleDebug :app:testDebugUnitTest -q
else
  echo "SKIP android: no hay Android SDK en esta máquina"
fi

if [ -f central/gradlew ]; then
  if [ "${OS:-}" = Windows_NT ] || /usr/libexec/java_home -v 25 >/dev/null 2>&1; then
    echo "== central"
    central/gradlew -p central build -q
  else
    echo "SKIP central: no hay JDK 25 en esta máquina"
  fi
fi
echo "SMOKE_PRODUCT_OK"
