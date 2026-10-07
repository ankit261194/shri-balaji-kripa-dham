import os
import sys
import shutil
import hashlib
import json
import urllib.request

apk_src = r"app\build\outputs\apk\release\app-release.apk"
if not os.path.exists(apk_src):
    print(f"Error: {apk_src} does not exist!")
    sys.exit(1)

apk_dst_backend = r"backend\downloads\ShriBalajiKripaDham-release.apk"
apk_dst_backend_149 = r"backend\downloads\ShriBalajiKripaDham-v149.apk"
os.makedirs(r"backend\downloads", exist_ok=True)

print(f"Copying {apk_src} to {apk_dst_backend} and {apk_dst_backend_149}...")
shutil.copy2(apk_src, apk_dst_backend)
shutil.copy2(apk_src, apk_dst_backend_149)

# Also copy to artifact directory
artifact_dir = r"C:\Users\hp\.gemini\antigravity\brain\4a9563ac-0aeb-420d-87e1-1333dc38f287"
shutil.copy2(apk_src, os.path.join(artifact_dir, "ShriBalajiKripaDham-release.apk"))
shutil.copy2(apk_src, os.path.join(artifact_dir, "ShriBalajiKripaDham-v149.apk"))

def get_md5(path):
    h = hashlib.md5()
    with open(path, "rb") as f:
        while chunk := f.read(8192 * 16):
            h.update(chunk)
    return h.hexdigest()

local_md5 = get_md5(apk_src)
local_size = os.path.getsize(apk_src)
print(f"Local APK size: {local_size} bytes ({local_size / (1024*1024):.2f} MB), MD5: {local_md5}")

# Reset any temp upload on server
try:
    with urllib.request.urlopen("https://shribalajikripadham.online/sync_apk.php?reset=1", timeout=30) as r:
        print("Server reset response:", r.read().decode())
except Exception as e:
    print("Reset notice:", e)

# Chunked upload
chunk_size = 5 * 1024 * 1024 # 5 MB
num_chunks = (local_size + chunk_size - 1) // chunk_size
print(f"Starting upload in {num_chunks} chunks...")

with open(apk_src, "rb") as f:
    for i in range(num_chunks):
        chunk_data = f.read(chunk_size)
        is_last = 1 if (i == num_chunks - 1) else 0
        url = f"https://shribalajikripadham.online/sync_apk.php?chunk_upload=1&chunk={i}&last={is_last}"
        req = urllib.request.Request(
            url,
            data=chunk_data,
            headers={
                "Content-Type": "application/octet-stream",
                "User-Agent": "Balaji-Chunk-Uploader/Build149"
            },
            method="POST"
        )
        try:
            with urllib.request.urlopen(req, timeout=120) as resp:
                res_text = resp.read().decode("utf-8")
                print(f"Chunk {i+1}/{num_chunks}: {res_text}")
        except Exception as e:
            print(f"Error on chunk {i+1}: {e}")
            sys.exit(1)

print("APK Chunks successfully uploaded!")

# Sync version.json
with open("version.json", "r", encoding="utf-8") as f:
    v_content = f.read()

req_v = urllib.request.Request(
    "https://shribalajikripadham.online/sync_apk.php?sync_version=1",
    data=v_content.encode("utf-8"),
    headers={"Content-Type": "application/json"},
    method="POST"
)
try:
    with urllib.request.urlopen(req_v, timeout=30) as resp:
        print("version.json sync response:", resp.read().decode("utf-8"))
except Exception as e:
    print("Error syncing version.json:", e)

# Verify live version.json
req_chk = urllib.request.Request("https://shribalajikripadham.online/version.json", headers={"Cache-Control": "no-cache"})
with urllib.request.urlopen(req_chk, timeout=30) as resp:
    live_ver = json.loads(resp.read().decode("utf-8"))
    print("Live Hostinger version.json:")
    print(json.dumps(live_ver, indent=2, ensure_ascii=False))
    assert live_ver.get("version_code") == 149, "Version code is not 149!"

# Verify APK download header & size
req_apk = urllib.request.Request("https://shribalajikripadham.online/downloads/ShriBalajiKripaDham-release.apk", method="HEAD")
with urllib.request.urlopen(req_apk, timeout=30) as resp:
    remote_len = int(resp.headers.get("Content-Length", 0))
    print(f"Live APK Header Content-Length: {remote_len} bytes")
    assert remote_len == local_size, f"Size mismatch! remote={remote_len}, local={local_size}"

print("\nSUCCESS: Build 149 (v2.64.00) is LIVE and verified on Hostinger CDN!")
