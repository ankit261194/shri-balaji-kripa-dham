import urllib.request
import urllib.parse
import json

queries = [
    'Aarti Kunj Bihari Ki',
    'Aarti Shri Ramayanji Ki',
    'Aarti Ramayan Ji Ki',
    'Khatu Shyam Ji Ki Aarti',
    'Om Jai Shri Shyam Hare',
    'Satyanarayan Ji Ki Aarti',
    'Jai Lakshmi Ramana',
    'Vindheshwari Chalisa',
    'Vindhyeshwari Chalisa',
    'Pretraj Chalisa',
    'Pretraj Sarkar',
    'Meri Naav Chali',
    'Bajrang Bali Meri Naav',
    'Aaj Mangalwar Hai',
    'Veer Hanumant',
    'Shabar Mantra Hanuman'
]

results = {}

for q in queries:
    url = f"https://archive.org/advancedsearch.php?q={urllib.parse.quote(q)}&fl[]=identifier,title,mediatype&rows=5&output=json"
    try:
        req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
        with urllib.request.urlopen(req, timeout=10) as resp:
            data = json.loads(resp.read().decode('utf-8'))
            docs = data.get("response", {}).get("docs", [])
            results[q] = docs
    except Exception as e:
        results[q] = f"Error: {e}"

with open("scratch/search_missing_results.json", "w", encoding="utf-8") as f:
    json.dump(results, f, indent=2, ensure_ascii=False)

print("Search completed. Saved to scratch/search_missing_results.json")
