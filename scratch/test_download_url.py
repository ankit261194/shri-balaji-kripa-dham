import urllib.request
import ssl
import sys

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

urls = [
    "https://shribalajikripadham.online/download.php",
    "https://shribalajikripadham.online/downloads/ShriBalajiKripaDham-release.apk",
    "https://shribalajikripadham.online/version.json",
    "https://shribalajikripadham.online/app_update.json"
]

class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        print(f"  --> Redirect code {code} to: {newurl}")
        return None

opener = urllib.request.build_opener(NoRedirect, urllib.request.HTTPSHandler(context=ctx))

for url in urls:
    print("Testing:", url)
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"})
    try:
        with opener.open(req, timeout=10) as resp:
            print(f"Status: {resp.status}")
            print(f"Headers: {dict(resp.headers)}")
            sample = resp.read(256)
            print(f"Sample length: {len(sample)}")
    except urllib.error.HTTPError as e:
        print(f"HTTPError: {e.code} - {e.reason}")
        print(f"Headers: {dict(e.headers)}")
        try:
            body = e.read(512).decode('utf-8', errors='ignore')
            print(f"Body: {body[:200]}")
        except Exception:
            pass
    except Exception as e:
        print(f"Error: {e}")
    print("-" * 50)
