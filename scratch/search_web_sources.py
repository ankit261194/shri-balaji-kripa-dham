import urllib.request
import urllib.parse
import re
import json

def get_page(url):
    req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'})
    with urllib.request.urlopen(req, timeout=10) as resp:
        return resp.read().decode('utf-8', errors='ignore')

# Let's search pagalnew for tracks
tracks_to_find = [
    'Aarti Shri Ramayan Ji Ki',
    'Aarti Kunj Bihari Ki',
    'Om Jai Shri Shyam Hare',
    'Jai Lakshmi Ramana',
    'Shri Vindhweshwari Chalisa',
    'Pretraj Chalisa'
]

for t in tracks_to_find:
    try:
        search_url = f"https://pagalnew.com/search.php?find={urllib.parse.quote(t)}"
        html = get_page(search_url)
        # find song links
        song_pages = re.findall(r'href="([^"]*/songs/[^"]+)"', html)
        if not song_pages:
            song_pages = re.findall(r'href="([^"]*\.html)"', html)
        print(f"=== {t} ===")
        print(song_pages[:3])
    except Exception as e:
        print(f"Error {t}: {e}")
