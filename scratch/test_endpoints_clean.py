import urllib.request
import ssl
import sys

sys.stdout.reconfigure(encoding='utf-8')
ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

urls = [
    "https://shribalajikripadham.online/sync_apk.php?status=1",
    "https://shribalajikripadham.online/sync_apk.php?reset=1",
    "https://shribalajikripadham.online/download.php"
]

for u in urls:
    print("Testing:", u)
    try:
        req = urllib.request.Request(u, headers={"User-Agent": "Mozilla/5.0"})
        with urllib.request.urlopen(req, context=ctx, timeout=15) as resp:
            print("Status:", resp.status)
            print(resp.read().decode('utf-8'))
    except urllib.error.HTTPError as e:
        print(f"HTTPError {e.code}: Location: {e.headers.get('Location')}")
        print(e.read().decode('utf-8'))
    except Exception as e:
        print("Error:", e)
    print("=" * 60)
