import urllib.request
import urllib.parse
import json

terms = [
    'ramayan aarti',
    'shri ramayan ji ki',
    'shyam aarti',
    'khatu shyam',
    'satyanarayan aarti',
    'satyanarayan bhagwan ki aarti',
    'vindhyavasini',
    'vindheshwari',
    'pretraj',
    'balaji bhajan',
    'hanuman bhajan'
]

results = {}
for t in terms:
    url = f"https://archive.org/advancedsearch.php?q={urllib.parse.quote(t)}+AND+mediatype:audio&fl[]=identifier,title&rows=10&output=json"
    req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            data = json.loads(resp.read().decode('utf-8'))
            results[t] = data.get('response', {}).get('docs', [])
    except Exception as e:
        results[t] = str(e)

with open("scratch/search_aartis_more_results.json", "w", encoding="utf-8") as out:
    json.dump(results, out, indent=2, ensure_ascii=False)

print("Search finished, saved to scratch/search_aartis_more_results.json")
