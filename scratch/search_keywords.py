import urllib.request
import urllib.parse
import json

keywords = ['Jagdish', 'Kunj', 'Ramayan', 'Satyanarayan', 'Shyam', 'Vindhy', 'Pretraj', 'Mangalwar', 'Bajrang']

with open("scratch/search_kw_results.txt", "w", encoding="utf-8") as out:
    for kw in keywords:
        url = f"https://archive.org/advancedsearch.php?q=mediatype:audio+AND+{kw}&fl[]=identifier,title&rows=15&output=json"
        try:
            req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
            with urllib.request.urlopen(req, timeout=10) as resp:
                data = json.loads(resp.read().decode('utf-8'))
                docs = data.get('response', {}).get('docs', [])
                out.write(f"\n=== KW: {kw} ({len(docs)}) ===\n")
                for d in docs:
                    out.write(f"  {d.get('identifier')} : {d.get('title')}\n")
        except Exception as e:
            out.write(f"Error {kw}: {e}\n")

print("Keywords search done. Written to scratch/search_kw_results.txt")
