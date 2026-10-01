# Google Play internal testing build

The existing Android build workflow still supplies a debug APK for direct installation.
Google Play bundle is a separate, manual workflow for a signed release AAB. It runs only
from main and does not upload to Play or publish a release. Release builds disable the
existing debug purchase simulation; use Google Play license testers for billing tests.

Select `internalTest` in the Google Play bundle workflow to include a visible
**TESTER: Unlock full story** button at the Chapter 10 wall. It opens Chapters 10–30
and ten save slots without payment, persists across app restarts, and never records a
purchase. The separate tester preference is ignored by normal `release` builds.
Select `release` for production; never promote an internalTest bundle to production.
Google Play tracks do not change the compiled build flag. Real billing remains available
in both builds so license testers can test it before using the tester unlock.

## One-time upload key setup on Windows

Check for Java's keytool in PowerShell: `Get-Command keytool`.
If it is unavailable, install a JDK such as Eclipse Temurin 17 from
https://adoptium.net/temurin/releases/?version=17 and reopen PowerShell.
Android Studio is not required.

Create a private folder outside the repository and generate an RSA upload key:

```powershell
New-Item -ItemType Directory -Force "$env:USERPROFILE\JailerSigning"
keytool -genkeypair -v -storetype JKS -keystore "$env:USERPROFILE\JailerSigning\jailer-upload.jks" -alias jailer-upload -keyalg RSA -keysize 3072 -validity 10000
```

Choose a strong password at the prompt. Press Enter at the key-password prompt to reuse
the keystore password. Supply accurate certificate details when asked. Keep the keystore
and passwords backed up securely; never commit them or send them in chat. If that file
already exists, stop and identify it before creating a replacement.

Under repository Settings > Secrets and variables > Actions, add:

| Secret | Value |
| --- | --- |
| ANDROID_UPLOAD_KEYSTORE_BASE64 | Base64 of the complete keystore file |
| ANDROID_UPLOAD_STORE_PASSWORD | Keystore password |
| ANDROID_UPLOAD_KEY_ALIAS | jailer-upload |
| ANDROID_UPLOAD_KEY_PASSWORD | Key password (same if Enter was used above) |

Copy the base64 directly to the clipboard without displaying it:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("$env:USERPROFILE\JailerSigning\jailer-upload.jks")) | Set-Clipboard
```

Paste it into the first secret's Value field. Clear the clipboard afterward with
`Set-Clipboard -Value "cleared"`. Repository secrets are visible to trusted workflows with
access to them; restrict repository write access accordingly.

## Build and upload

Builds compile and target Android 16 (API 36), using Android Gradle Plugin 8.10.1,
Gradle 8.11.1 and JDK 17. The minimum supported Android version remains API 26.

If Play has already accepted version code 1, use code 2 for the API 36 replacement,
even if the first internal release was blocked before publication. Remove the old
API 35 bundle from the draft release and upload the new signed AAB.

1. Merge the release workflow PR once its debug and release checks pass.
2. Open Actions > Google Play bundle > Run workflow. Select main and choose `internalTest` for testers or `release` for production.
3. Enter version code 1 for the first Play upload. Increase it for each subsequent upload;
   a rerun with the same number does not create a new version code.
4. Download the `the-last-jailer-play-<build_kind>-<code>` artifact from the successful run and extract
   `app-internalTest.aab` (testers) or `app-release.aab` (production) from the ZIP.
5. Upload that AAB to the internal testing release. Complete Play App Signing as prompted.
6. Review Console validation, add release notes, and roll out to internal testers when ready.

Signing secrets are scoped to the signing step, and the temporary key is removed on exit.
Only the signed AAB and release test report are uploaded. PR checks build an unsigned
release bundle and run release tests without access to the upload key.

The Play-installed app may need to replace a debug-signed installation with the same package
name. Preserve any important saves before uninstalling; do not assume they survive uninstall.

Billing still needs a merchant payments profile, an active one-time product with ID
`unlock_full_story`, the intended UK price of £1.99, and a license tester account. Those
Console settings and an actual Play billing test remain separate from a successful build.
