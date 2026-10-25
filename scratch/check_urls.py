import re
import urllib.request

kt_file = r"app/src/main/java/com/example/shribalajikripadham/data/sacred/SacredTracksData.kt"
with open(kt_file, "r", encoding="utf-8") as f:
    text = f.read()

pattern = re.compile(r'SacredTrack\s*\(\s*id\s*=\s*(\d+),\s*trackKey\s*=\s*"([^"]+)",\s*titleHindi\s*=\s*"([^"]+)",.*?audioUrl\s*=\s*"([^"]*)".*?lyricsHindi', re.DOTALL)
for m in pattern.finditer(text):
    track_id, key, title, url = m.groups()
    # Check if URL is valid or 404
    status = "UNKNOWN"
    if url.startswith("http"):
        try:
            req = urllib.request.Request(url, method="HEAD", headers={"User-Agent": "Mozilla/5.0"})
            with urllib.request.urlopen(req, timeout=3) as resp:
                status = f"HTTP {resp.status} ({resp.headers.get('Content-Length', '?')} bytes)"
        except Exception as e:
            status = f"ERROR: {e}"
    else:
        status = "EMPTY / LOCAL"
    safe_title = title.encode("ascii", "replace").decode("ascii")
    print(f"ID: {track_id} | Key: {key:30} | Status: {status} | URL: {url}")
