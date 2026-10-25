import sys
import urllib.request
import json

sys.stdout.reconfigure(encoding='utf-8')

print("Checking live Hostinger CDN status...")

# 1. Check version.json
req_chk = urllib.request.Request("https://shribalajikripadham.online/version.json", headers={"Cache-Control": "no-cache"})
with urllib.request.urlopen(req_chk, timeout=30) as resp:
    live_ver = json.loads(resp.read().decode("utf-8"))
    print("Live Hostinger version.json:")
    print(json.dumps(live_ver, indent=2, ensure_ascii=False))
    assert live_ver.get("version_code") == 153, f"Version code is not 153! Found: {live_ver.get('version_code')}"
    print("VERIFIED: version.json has version_code = 153!")

# 2. Check APK download header
req_apk = urllib.request.Request("https://shribalajikripadham.online/downloads/ShriBalajiKripaDham-release.apk", method="HEAD")
with urllib.request.urlopen(req_apk, timeout=30) as resp:
    remote_len = int(resp.headers.get("Content-Length", 0))
    print(f"Hostinger CDN APK Header Length: {remote_len} bytes ({remote_len / (1024*1024):.2f} MB)")
    assert remote_len == 84654110, f"Size mismatch! Expected 84654110, got {remote_len}"
    print("VERIFIED: APK size matches 100% (84,654,110 bytes)!")
