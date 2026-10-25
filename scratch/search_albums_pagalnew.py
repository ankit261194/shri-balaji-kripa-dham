import urllib.request
import urllib.parse
import re

queries = ['aarti sangrah', 'chalisa sangrah', 'shyam', 'khatu', 'satyanarayan', 'durga']

for q in queries:
    url = f"https://pagalnew.com/search.php?find={urllib.parse.quote(q)}"
    try:
        req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'})
        with urllib.request.urlopen(req, timeout=10) as resp:
            html = resp.read().decode('utf-8', errors='ignore')
            albums = re.findall(r'href="([^"]*/album/[^"]+)"', html)
            print(f"=== {q} ({len(albums)} albums) ===")
            for a in list(set(albums))[:5]:
                print(" ", a)
    except Exception as e:
        print(f"Error {q}: {e}")
