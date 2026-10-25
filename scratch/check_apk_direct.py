import urllib.request
import ssl
import json

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

# Let's check with HEAD request
head_req = urllib.request.Request("https://shribalajikripadham.online/downloads/ShriBalajiKripaDham-release.apk", method="HEAD", headers={"User-Agent": "Mozilla/5.0"})
try:
    with urllib.request.urlopen(head_req, context=ctx, timeout=15) as resp:
        print("HEAD resp status:", resp.status)
        print("HEAD headers:", dict(resp.headers))
except urllib.error.HTTPError as e:
    print(f"HEAD HTTPError: {e.code} - {e.reason}")
    print("HEAD headers:", dict(e.headers))
except Exception as e:
    print("HEAD error:", e)

# Let's check with GET request without redirect following
class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        print(f"GET NoRedirect intercepted code {code} -> {newurl}")
        return None

opener = urllib.request.build_opener(NoRedirect, urllib.request.HTTPSHandler(context=ctx))
get_req = urllib.request.Request("https://shribalajikripadham.online/downloads/ShriBalajiKripaDham-release.apk", headers={"User-Agent": "Mozilla/5.0"})
try:
    with opener.open(get_req, timeout=15) as resp:
        print("GET resp status:", resp.status)
        print("GET headers:", dict(resp.headers))
except urllib.error.HTTPError as e:
    print(f"GET HTTPError: {e.code} - {e.reason}")
    print("GET headers:", dict(e.headers))
except Exception as e:
    print("GET error:", e)
