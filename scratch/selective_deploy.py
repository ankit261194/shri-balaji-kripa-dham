import sys
import urllib.request
import json

sys.stdout.reconfigure(encoding='utf-8')

url = 'https://shribalajikripadham.online/deploy.php?file=deploy.php,api/fetch_saptashati_audio.php'
req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
with urllib.request.urlopen(req, timeout=30) as resp:
    data = json.loads(resp.read().decode('utf-8'))
    print("Selective sync response:")
    print(json.dumps(data, indent=2, ensure_ascii=False))
