#!/usr/bin/env sh
set -eu

version="0.9.2"
base_url="https://repo.maven.apache.org/maven2/top/yukonga/miuix/kmp"
root_dir="$(pwd)"
output_dir="$root_dir/app/libs"
tmp_dir="${TMPDIR:-/tmp}/verbo-miuix-aar"

artifacts="
miuix-core-android
miuix-shader-android
miuix-squircle-android
miuix-ui-android
miuix-icons-android
miuix-preference-android
"

rm -rf "$tmp_dir"
mkdir -p "$tmp_dir" "$output_dir"

for artifact in $artifacts; do
  aar="$artifact-$version.aar"
  source="$tmp_dir/$aar"
  work="$tmp_dir/$artifact"
  target="$output_dir/$aar"

  curl -fsSL -o "$source" "$base_url/$artifact/$version/$aar"
  rm -rf "$work"
  mkdir -p "$work"
  unzip -q "$source" -d "$work"

  metadata="$work/META-INF/com/android/build/gradle/aar-metadata.properties"
  if [ -f "$metadata" ]; then
    sed -i 's/^minCompileSdk=.*/minCompileSdk=36/' "$metadata"
  fi

  rm -f "$target"
  (cd "$work" && jar cf "$target" .)
done

echo "Patched Miuix Android artifacts are ready in $output_dir."
