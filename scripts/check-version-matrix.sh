#!/usr/bin/env sh
set -eu

check_contains() {
  file="$1"
  needle="$2"
  if ! grep -Fq "$needle" "$file"; then
    echo "Version matrix mismatch: expected '$needle' in $file" >&2
    exit 1
  fi
}

check_contains build.gradle.kts 'id("com.android.application") version "9.2.1"'
check_contains build.gradle.kts 'id("org.jetbrains.kotlin.android") version "2.4.0"'
check_contains build.gradle.kts 'id("org.jetbrains.kotlin.plugin.compose") version "2.4.0"'
check_contains build.gradle.kts 'id("org.jetbrains.compose") version "1.11.1"'
check_contains app/build.gradle.kts 'compileSdk = 37'
check_contains app/build.gradle.kts 'targetSdk = 37'
check_contains app/build.gradle.kts 'buildToolsVersion = "37.0.0"'
check_contains app/build.gradle.kts 'androidx.activity:activity-ktx:1.13.0'
check_contains app/build.gradle.kts 'top.yukonga.miuix.kmp:miuix-ui-android:0.9.2'
check_contains app/build.gradle.kts 'com.tom-roush:pdfbox-android:2.0.27.0'
check_contains app/build.gradle.kts 'com.squareup.okhttp3:okhttp:5.4.0'
check_contains app/build.gradle.kts 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0'
check_contains app/build.gradle.kts 'org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0'
check_contains .github/workflows/build.yml 'gradle-version: 9.5.1'
check_contains .github/workflows/build.yml 'java-version: "21"'
check_contains .github/workflows/build.yml 'platforms;android-37'
check_contains .github/workflows/build.yml 'build-tools;37.0.0'

echo "Version matrix is pinned."
