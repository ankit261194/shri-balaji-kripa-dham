import sys
import urllib.request
import json
import time

sys.stdout.reconfigure(encoding='utf-8')

print("1. Calling deploy.php to sync backend files from GitHub...")
req_dep = urllib.request.Request("https://shribalajikripadham.online/deploy.php?sha=main", headers={"User-Agent": "Mozilla/5.0"})
with urllib.request.urlopen(req_dep, timeout=45) as resp:
    res = json.loads(resp.read().decode('utf-8'))
    print("deploy.php synced successfully! Synced files count:", len(res.get("synced_files", [])))

time.sleep(2)

print("\n2. Calling fetch_saptashati_audio.php to download all 17 chapters directly to Hostinger storage...")
req_fetch = urllib.request.Request("https://shribalajikripadham.online/api/fetch_saptashati_audio.php", headers={"User-Agent": "Mozilla/5.0"})
with urllib.request.urlopen(req_fetch, timeout=300) as resp:
    res = json.loads(resp.read().decode('utf-8'))
    print("fetch_saptashati_audio.php response:")
    print(json.dumps(res, indent=2, ensure_ascii=False))

