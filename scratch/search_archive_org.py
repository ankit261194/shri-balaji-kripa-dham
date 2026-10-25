import urllib.request
import urllib.parse
import json

queries = [
    ("aaj_mangalwar", "title:(Aaj Mangalwar Hai) OR title:(Mangalwar Hai Mahaveer)"),
    ("meri_naav", "title:(Meri Naav Chali) OR title:(Naav Chali Bajrang Bali)"),
    ("shabar_hanuman", "title:(Hanuman Shabar) OR title:(Veer Hanumant Mantra) OR title:(Shabar Mantra)"),
    ("laxmi_ramana", "title:(Laxmi Ramana) OR title:(Lakshmi Ramana) OR title:(Satyanarayan Aarti)")
]

for key, q in queries:
    url = f"https://archive.org/advancedsearch.php?q={urllib.parse.quote(q)}%20AND%20mediatype:(audio)&fl[]=identifier,title,creator,publicdate&rows=10&page=1&output=json"
    req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            data = json.loads(resp.read().decode('utf-8'))
            docs = data.get('response', {}).get('docs', [])
            print(f"=== {key} ({len(docs)} found) ===")
            for d in docs[:5]:
                print(f"  [{d.get('identifier')}]: {d.get('title')} by {d.get('creator')}")
    except Exception as e:
        print(f"Error {key}: {e}")
