#!/usr/bin/env bash
set -euo pipefail

bundle_path="${1:?Pass the release AAB path}"
test -s "$bundle_path"
for secret_name in ANDROID_UPLOAD_KEYSTORE_BASE64 ANDROID_UPLOAD_STORE_PASSWORD ANDROID_UPLOAD_KEY_ALIAS ANDROID_UPLOAD_KEY_PASSWORD; do
  if [[ -z "${!secret_name:-}" ]]; then
    echo "Missing GitHub Actions secret: $secret_name" >&2
    exit 1
  fi
done

# The private key exists only in this step's temporary directory, never an artifact.
umask 077
signing_dir="$(mktemp -d "${RUNNER_TEMP:-${TMPDIR:-/tmp}}/jailer-signing.XXXXXX")"
trap 'rm -rf "$signing_dir"' EXIT
printf '%s' "$ANDROID_UPLOAD_KEYSTORE_BASE64" | base64 --decode > "$signing_dir/upload.jks"
test -s "$signing_dir/upload.jks"

jarsigner -keystore "$signing_dir/upload.jks" \
  -storepass:env ANDROID_UPLOAD_STORE_PASSWORD \
  -keypass:env ANDROID_UPLOAD_KEY_PASSWORD \
  "$bundle_path" "$ANDROID_UPLOAD_KEY_ALIAS"

# Trust only our upload certificate; strict verification rejects unsigned entries,
# damaged content, or a signer that does not match the requested alias.
jarsigner -verify -strict -keystore "$signing_dir/upload.jks" \
  -storepass:env ANDROID_UPLOAD_STORE_PASSWORD \
  "$bundle_path" "$ANDROID_UPLOAD_KEY_ALIAS"
