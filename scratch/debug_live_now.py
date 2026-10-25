import urllib.request
import ssl
import sys
import json

sys.stdout.reconfigure(encoding='utf-8')
ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        print(f"  [Redirect {code}] -> {newurl}")
        return None

opener = urllib.request.build_opener(NoRedirect, urllib.request.HTTPSHandler(context=ctx))

def check(url):
    print(f"\nChecking: {url}")
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"})
    try:
        with opener.open(req, timeout=10) as resp:
            print(f"Status: {resp.status}")
            print(f"Content-Type: {resp.headers.get('Content-Type')}")
            print(f"Content-Length: {resp.headers.get('Content-Length')}")
            print(f"Location: {resp.headers.get('Location')}")
            body = resp.read(300)
            print(f"Body: {body[:150]}")
    except urllib.error.HTTPError as e:
        print(f"HTTPError {e.code}")
        print(f"Location: {e.headers.get('Location')}")
        body = e.read(300)
        print(f"Body: {body[:150]}")
    except Exception as e:
        print(f"Error: {e}")

check("https://shribalajikripadham.online/sync_apk.php?status=1")
check("https://shribalajikripadham.online/download.php")
check("https://shribalajikripadham.online/downloads/ShriBalajiKripaDham-release.apk")
