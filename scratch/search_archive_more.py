import urllib.request
import urllib.parse
import json

def s(q):
    url = f"https://archive.org/advancedsearch.php?q={urllib.parse.quote(q)}%20AND%20mediatype:(audio)&fl[]=identifier,title&rows=10&output=json"
    req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
    try:
        data = json.loads(urllib.request.urlopen(req, timeout=10).read().decode('utf-8'))
        docs = data.get('response', {}).get('docs', [])
        print(f"=== Results for '{q}' ({len(docs)} found) ===")
        for d in docs:
            print(f"   {d.get('identifier')}: {d.get('title')}")
    except Exception as e:
        print(f"Error {q}: {e}")

s("Satyanarayan Aarti")
s("Om Jai Lakshmi Ramana")
s("Aaj Mangalwar")
s("Meri Naav Chali")
s("Hanuman Shabar")
s("Veer Hanumant")
