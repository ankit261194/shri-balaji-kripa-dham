import urllib.request
import re

url = "https://pagalnew.com/search.php?find=aarti"
req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'})
with urllib.request.urlopen(req, timeout=10) as resp:
    html = resp.read().decode('utf-8', errors='ignore')
    links = re.findall(r'href="([^"]*/songs/[^"]+)"', html)
    for l in links[:30]:
        print(l)
