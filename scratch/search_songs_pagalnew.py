import urllib.request
import urllib.parse
import re

queries = ['vishnu aarti', 'satyanarayan', 'khatu', 'shyam hare', 'krishna aarti', 'hanuman', 'ramayan']

for q in queries:
    url = f"https://pagalnew.com/search.php?find={urllib.parse.quote(q)}"
    try:
        req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'})
        with urllib.request.urlopen(req, timeout=10) as resp:
            html = resp.read().decode('utf-8', errors='ignore')
            songs = re.findall(r'href="([^"]*/songs/[^"]+)"', html)
            print(f"=== {q} ({len(songs)} songs) ===")
            for s in list(set(songs))[:5]:
                print(" ", s)
    except Exception as e:
        print(f"Error {q}: {e}")
