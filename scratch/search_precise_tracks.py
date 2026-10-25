import urllib.request
import urllib.parse
import json

queries = [
    ("kunjbihari", "kunjbihari OR kunj bihari"),
    ("ramayan", "ramayan aarti OR aarti ramayan"),
    ("shyam", "khatu shyam aarti OR om jai shri shyama hare"),
    ("satyanarayan", "satyanarayan aarti OR jai lakshmi ramana"),
    ("vindhyashwari", "vindhweshwari OR vindhyavasini"),
    ("pretraj", "pretraj OR pret raj"),
    ("naav_chali", "meri naav chali OR meri nav chali"),
    ("aaj_mangalwar", "aaj mangalwar hai OR mangalwar mahaveer"),
    ("veer_shabar", "veer hanuman OR shabar mantra")
]

for tag, q in queries:
    url = f"https://archive.org/advancedsearch.php?q={urllib.parse.quote(q)}+AND+mediatype:audio&fl[]=identifier,title&rows=10&output=json"
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
    try:
        with urllib.request.urlopen(req, timeout=10) as r:
            data = json.loads(r.read().decode("utf-8"))
            docs = data.get("response", {}).get("docs", [])
            print(f"=== {tag} ({len(docs)} found) ===")
            for d in docs:
                ident = d.get("identifier")
                title = d.get("title", "").encode("ascii", "replace").decode("ascii")
                print(f"  {ident} : {title}")
    except Exception as e:
        print(f"Error {tag}: {e}")
