import urllib.request
import re

for vol in [1, 2, 3, 4, 5]:
    url = f"https://pagalnew.com/album/aarti-vol-{vol}.html"
    try:
        req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'})
        with urllib.request.urlopen(req, timeout=10) as resp:
            html = resp.read().decode('utf-8', errors='ignore')
            songs = re.findall(r'href="([^"]*/songs/[^"]+)"', html)
            print(f"=== Vol {vol} ({len(songs)} songs) ===")
            for s in set(songs):
                print(" ", s)
    except Exception as e:
        print(f"Vol {vol} error: {e}")
