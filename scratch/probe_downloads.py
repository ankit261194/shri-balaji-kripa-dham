import urllib.request
import ssl

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None

opener = urllib.request.build_opener(NoRedirect, urllib.request.HTTPSHandler(context=ctx))

test_files = [
    "downloads/ShriBalajiKripaDham-release.apk",
    "downloads/ShriBalajiKripaDham-v168.apk",
    "downloads/ShriBalajiKripaDham-v167.apk",
    "downloads/ShriBalajiKripaDham-v166.apk",
    "downloads/ShriBalajiKripaDham-v165.apk",
    "downloads/ShriBalajiKripaDham-v164.apk",
    "downloads/ShriBalajiKripaDham-v107.apk",
    "downloads/test.txt"
]

for f in test_files:
    url = f"https://shribalajikripadham.online/{f}"
    req = urllib.request.Request(url, method="HEAD", headers={"User-Agent": "Mozilla/5.0"})
    try:
        resp = opener.open(req, timeout=10)
        print(f"{f}: status {resp.status}, length: {resp.headers.get('Content-Length')}, type: {resp.headers.get('Content-Type')}")
    except urllib.error.HTTPError as e:
        loc = e.headers.get('Location', '')
        x_pow = e.headers.get('X-Powered-By', '')
        print(f"{f}: HTTP {e.code} -> Loc: {loc} (X-Powered-By: {x_pow})")
    except Exception as e:
        print(f"{f}: Error {e}")
