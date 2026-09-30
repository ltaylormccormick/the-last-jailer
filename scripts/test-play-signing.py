"""Exercise signing without using the real upload key or producing a release artifact."""
import base64
import os
from pathlib import Path
import subprocess
import tempfile
import zipfile

script = Path(__file__).with_name("sign-play-bundle.sh").resolve()
with tempfile.TemporaryDirectory(prefix="jailer-signing-check-") as folder:
    root = Path(folder)
    key = root / "test.jks"
    bundle = root / "test.aab"
    env = os.environ.copy()
    env.pop("ANDROID_UPLOAD_KEYSTORE_BASE64", None)
    env.update(
        ANDROID_UPLOAD_STORE_PASSWORD="disposable-test-password",
        ANDROID_UPLOAD_KEY_PASSWORD="disposable-test-password",
        ANDROID_UPLOAD_KEY_ALIAS="test-upload",
        RUNNER_TEMP=folder,
    )
    subprocess.run([
        "keytool", "-genkeypair", "-noprompt", "-storetype", "JKS", "-keystore", str(key),
        "-storepass:env", "ANDROID_UPLOAD_STORE_PASSWORD",
        "-keypass:env", "ANDROID_UPLOAD_KEY_PASSWORD", "-alias", "test-upload",
        "-keyalg", "RSA", "-keysize", "2048", "-validity", "10000",
        "-dname", "CN=Disposable signing test",
    ], env=env, check=True, capture_output=True)
    with zipfile.ZipFile(bundle, "w") as archive:
        archive.writestr("base/test.txt", "test payload")

    def sign():
        return subprocess.run(["bash", str(script), str(bundle)], env=env, capture_output=True, text=True)

    missing = sign()
    assert missing.returncode != 0 and "Missing GitHub Actions secret" in missing.stderr
    env["ANDROID_UPLOAD_KEYSTORE_BASE64"] = base64.b64encode(key.read_bytes()).decode()
    signed = sign()
    assert signed.returncode == 0, signed.stdout + signed.stderr
    assert not list(root.glob("jailer-signing.*"))
    with zipfile.ZipFile(bundle) as archive:
        assert any(name.endswith(".SF") for name in archive.namelist())

    env["ANDROID_UPLOAD_KEY_PASSWORD"] = "wrong-password"
    assert sign().returncode != 0
    assert not list(root.glob("jailer-signing.*"))
    env["ANDROID_UPLOAD_KEY_PASSWORD"] = "disposable-test-password"
    with zipfile.ZipFile(bundle, "a") as archive:
        archive.writestr("base/unsigned.txt", "must be rejected")
    verification = subprocess.run([
        "jarsigner", "-verify", "-strict", "-keystore", str(key),
        "-storepass:env", "ANDROID_UPLOAD_STORE_PASSWORD", str(bundle), "test-upload",
    ], env=env, capture_output=True)
    assert verification.returncode != 0, "Strict verification must reject unsigned additions"
print("Disposable-key signing, strict verification, failure handling and key cleanup passed")
