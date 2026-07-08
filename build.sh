#!/bin/bash
# Script de build local para ConstruccionIA
# Uso: ./build.sh [release|debug|test|lint]

set -e

JAVA_HOME="C:\Program Files\Android\Android Studio1\jbr"
export JAVA_HOME

case "${1:-debug}" in
  debug)
    echo "🔨 Building debug APK..."
    ./gradlew assembleDebug
    echo "✅ APK: app/build/outputs/apk/debug/app-debug.apk"
    ;;
  release)
    echo "🔨 Building release APK..."
    ./gradlew assembleRelease
    echo "✅ APK: app/build/outputs/apk/release/app-release-unsigned.apk"
    ;;
  test)
    echo "🧪 Running unit tests..."
    ./gradlew testDebugUnitTest
    echo "✅ Report: app/build/reports/tests/testDebugUnitTest/"
    ;;
  lint)
    echo "🔍 Running lint..."
    ./gradlew lintDebug
    echo "✅ Report: app/build/reports/lint-results-debug.html"
    ;;
  *)
    echo "Usage: ./build.sh [debug|release|test|lint]"
    exit 1
    ;;
esac
