import urllib.request
import ssl
import json

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

params = [
    "",
    "?reset=1",
    "?sync_version=1",
    "?chunk_upload=1",
    "?action=status",
]

for p in params:
    url = f"https://shribalajikripadham.online/sync_apk.php{p}"
    print(f"Testing {url}:")
    try:
        req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
        with urllib.request.urlopen(req, context=ctx, timeout=15) as resp:
            print(f"Status: {resp.status}")
            txt = resp.read().decode('utf-8', errors='ignore')
            print(f"Response: {txt[:300]}")
    except urllib.error.HTTPError as e:
        print(f"HTTPError: {e.code}")
        txt = e.read().decode('utf-8', errors='ignore')
        print(f"Body: {txt[:300]}")
    except Exception as e:
        print(f"Error: {e}")
    print("-" * 50)
