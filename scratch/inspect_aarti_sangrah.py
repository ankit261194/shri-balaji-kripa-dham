import urllib.request
import re

url = "https://pagalnew.com/album/aarti-sangrah-2020.html"
req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'})
with urllib.request.urlopen(req, timeout=10) as resp:
    html = resp.read().decode('utf-8', errors='ignore')
    songs = re.findall(r'href="([^"]*/songs/[^"]+)"', html)
    print(f"Total songs: {len(songs)}")
    for s in list(set(songs)):
        print(" ", s)
