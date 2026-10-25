import urllib.request
import urllib.parse
import re

url = "https://pagalfree.com/search"
params = {'q': 'khatu shyam'}
full_url = f"{url}?{urllib.parse.urlencode(params)}"
req = urllib.request.Request(full_url, headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'})
try:
    with urllib.request.urlopen(req, timeout=10) as resp:
        html = resp.read().decode('utf-8', errors='ignore')
        songs = re.findall(r'href="([^"]*/music/[^"]+)"', html)
        print("Pagalfree songs:", songs)
except Exception as e:
    print("Pagalfree error:", e)
