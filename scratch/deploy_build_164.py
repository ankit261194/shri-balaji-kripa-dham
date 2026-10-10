import os
import sys
import shutil
import hashlib
import json
import urllib.request
import time

sys.stdout.reconfigure(encoding='utf-8')

apk_src = r"app\build\outputs\apk\release\app-release.apk"
if not os.path.exists(apk_src):
    print(f"Error: {apk_src} does not exist!")
    sys.exit(1)

apk_release = r"ShriBalajiKripaDham-release.apk"
apk_v164 = r"ShriBalajiKripaDham-v164.apk"

print(f"Copying {apk_src} to {apk_release} and {apk_v164}...")
shutil.copy2(apk_src, apk_release)
shutil.copy2(apk_src, apk_v164)

apk_dst_backend = r"backend\downloads\ShriBalajiKripaDham-release.apk"
apk_dst_backend_164 = r"backend\downloads\ShriBalajiKripaDham-v164.apk"
os.makedirs(r"backend\downloads", exist_ok=True)

shutil.copy2(apk_src, apk_dst_backend)
shutil.copy2(apk_src, apk_dst_backend_164)

# Copy to artifact directory
artifact_dir = r"C:\Users\hp\.gemini\antigravity\brain\4a9563ac-0aeb-420d-87e1-1333dc38f287"
shutil.copy2(apk_src, os.path.join(artifact_dir, "ShriBalajiKripaDham-release.apk"))
shutil.copy2(apk_src, os.path.join(artifact_dir, "ShriBalajiKripaDham-v164.apk"))

def get_md5(path):
    h = hashlib.md5()
    with open(path, "rb") as f:
        while chunk := f.read(8192 * 16):
            h.update(chunk)
    return h.hexdigest()

local_md5 = get_md5(apk_release)
local_size = os.path.getsize(apk_release)
print(f"Local APK size: {local_size} bytes ({local_size / (1024*1024):.2f} MB), MD5: {local_md5}")

# Reset any temp upload on server
try:
    with urllib.request.urlopen("https://shribalajikripadham.online/sync_apk.php?reset=1", timeout=30) as r:
        print("Server reset response:", r.read().decode())
except Exception as e:
    print("Reset notice:", e)

# Chunked upload (5 MB chunks)
chunk_size = 5 * 1024 * 1024
num_chunks = (local_size + chunk_size - 1) // chunk_size
print(f"Starting chunked upload in {num_chunks} chunks using chunk_upload=1...")

with open(apk_release, "rb") as f:
    for i in range(num_chunks):
        chunk_data = f.read(chunk_size)
        is_last = 1 if (i == num_chunks - 1) else 0
        upload_url = f"https://shribalajikripadham.online/sync_apk.php?chunk_upload=1&chunk={i}&last={is_last}"
        req = urllib.request.Request(upload_url, data=chunk_data, headers={
            "Content-Type": "application/octet-stream",
            "User-Agent": "Mozilla/5.0"
        })
        success = False
        for attempt in range(3):
            try:
                with urllib.request.urlopen(req, timeout=60) as resp:
                    resp_data = resp.read().decode('utf-8')
                    print(f"  Chunk {i+1}/{num_chunks} (last={is_last}): {resp_data}")
                    success = True
                    break
            except Exception as e:
                print(f"  Chunk {i+1} attempt {attempt+1} failed: {e}")
                time.sleep(2)
        if not success:
            print(f"Failed to upload chunk {i+1} after 3 attempts.")
            sys.exit(1)

# Update version.json on server
print("\nUpdating version.json on server to Build 164 (v2.77.0)...")
version_data = {
    "versionCode": 164,
    "versionName": "2.77.0",
    "apkUrl": "https://shribalajikripadham.online/downloads/ShriBalajiKripaDham-release.apk",
    "apk_url": "https://shribalajikripadham.online/downloads/ShriBalajiKripaDham-release.apk",
    "apkSize": f"{local_size / (1024*1024):.1f} MB",
    "releaseNotes": "1. 12 पावन थीम्स (शेरावाली मैया, दीपावली, वीर हनुमान, आदि) के दिव्य रूप, विशिष्ट देव-प्रतीक व पावन मंत्र\n2. अलौकिक दर्शन का पूर्ण चित्र (JPEG फ़ाइल) शेयरिंग\n3. दर्शन व्यूज वास्तविक व स्थिर\n4. दरबार लाइव केवल रविवार टोकन वितरण समय पर लाइव\n5. सभी कृत्रिम ऑडियो हटाकर केवल 100% प्रामाणिक पारंपरिक आरतियाँ व चालीसा",
    "forceUpdate": False
}

sync_version_url = "https://shribalajikripadham.online/sync_apk.php?sync_version=1"
req = urllib.request.Request(sync_version_url, data=json.dumps(version_data).encode('utf-8'), headers={
    "Content-Type": "application/json",
    "User-Agent": "Mozilla/5.0"
})
try:
    with urllib.request.urlopen(req, timeout=30) as resp:
        print("Sync version response:", resp.read().decode('utf-8'))
except Exception as e:
    print("Sync version error:", e)

# Also update local backend/version.json
with open(r"backend\version.json", "w", encoding="utf-8") as f:
    json.dump(version_data, f, indent=2, ensure_ascii=False)
print("Updated local backend/version.json")
