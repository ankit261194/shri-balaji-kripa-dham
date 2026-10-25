import urllib.request
import urllib.parse
import json

instances = [
    'https://yewtu.be',
    'https://inv.tux.pizza',
    'https://vid.puffyan.us',
    'https://invidious.nerdvpn.de',
    'https://invidious.projectsegfau.lt'
]

query = 'Bajrang Bali Meri Naav Chali'

for inst in instances:
    url = f"{inst}/api/v1/search?q={urllib.parse.quote(query)}"
    try:
        req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
        with urllib.request.urlopen(req, timeout=5) as resp:
            data = json.loads(resp.read().decode('utf-8'))
            print(f"SUCCESS {inst}: {len(data)} results")
            for item in data[:3]:
                print(f"  {item.get('videoId')}: {item.get('title')}")
            break
    except Exception as e:
        print(f"Failed {inst}: {e}")
