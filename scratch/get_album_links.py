import urllib.request
import re

url = 'https://pagalnew.com/songs/aarti-shri-ramayan-ji-ki-aarti-vol-3.html'
req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'})
with urllib.request.urlopen(req) as resp:
    html = resp.read().decode('utf-8', errors='ignore')
    albums = re.findall(r'href="([^"]*album[^"]*)"', html)
    print('Album links:', albums)
