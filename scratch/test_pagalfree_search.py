import urllib.request
import urllib.parse
import re

for q in ['khatu shyam', 'satyanarayan', 'pretraj', 'chalisa']:
    url = f"https://pagalfree.com/search?text={urllib.parse.quote(q)}"
    req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'})
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            html = resp.read().decode('utf-8', errors='ignore')
            links = re.findall(r'href="([^"]*/music/[^"]+)"', html)
            print(f"=== {q} ({len(links)} found) ===")
            for l in list(set(links))[:5]:
                print(" ", l)
    except Exception as e:
        print(f"Error {q}: {e}")
