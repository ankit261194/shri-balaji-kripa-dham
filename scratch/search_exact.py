import urllib.request
import urllib.parse
import json

queries = [
    'Kunj Bihari',
    'Ramayan Ji Ki',
    'Shyam Hare',
    'Satyanarayan',
    'Vindhy',
    'Pretraj',
    'Bajrang Bali',
    'Mahaveer'
]

with open("scratch/search_exact_results.txt", "w", encoding="utf-8") as out:
    for q in queries:
        url = f"https://archive.org/advancedsearch.php?q={urllib.parse.quote(q)}+AND+mediatype:audio&fl[]=identifier,title&rows=10&output=json"
        req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
        try:
            with urllib.request.urlopen(req, timeout=10) as resp:
                data = json.loads(resp.read().decode('utf-8'))
                docs = data.get("response", {}).get("docs", [])
                out.write(f"\n=== Query: {q} ({len(docs)} found) ===\n")
                for d in docs:
                    out.write(f"  {d.get('identifier')} : {d.get('title')}\n")
        except Exception as e:
            out.write(f"Error {q}: {e}\n")

print("Exact search written to scratch/search_exact_results.txt")
