import urllib.request
import json

url = "https://shribalajikripadham.online/api/migrate_tracks.php"
try:
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
    with urllib.request.urlopen(req, timeout=30) as resp:
        print("Status:", resp.status)
        print("Body:", resp.read().decode('utf-8'))
except Exception as e:
    print("Error:", e)
