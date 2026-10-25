import urllib.request
import urllib.parse
import json

terms = [
    ("laxmi", "Laxmi Aarti OR Lakshmi Aarti"),
    ("kunjbihari", "Kunj Bihari OR Kunjbihari"),
    ("ramayan", "Aarti Ramayan OR Ramayan Aarti"),
    ("khatu_shyam", "Khatu Shyam Aarti OR Shyama Hare"),
    ("satyanarayan", "Satyanarayan Aarti OR Jai Lakshmi Ramana"),
    ("vindhweshwari", "Vindhweshwari"),
    ("pretraj", "Pretraj"),
    ("meri_naav", "Meri Naav Chali OR Bajrang Bali"),
    ("aaj_mangalwar", "Aaj Mangalwar Hai OR Mahaveer"),
    ("veer_shabar", "Shabar Mantra OR Hanuman Shabar")
]

with open("scratch/search_results.txt", "w", encoding="utf-8") as out:
    for key, q in terms:
        url = f"https://archive.org/advancedsearch.php?q={urllib.parse.quote(q)}+AND+mediatype:audio&fl[]=identifier,title&rows=5&output=json"
        req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
        try:
            with urllib.request.urlopen(req, timeout=10) as r:
                data = json.loads(r.read().decode("utf-8"))
                docs = data.get("response", {}).get("docs", [])
                out.write(f"\n=== {key}: {q} ({len(docs)} found) ===\n")
                for d in docs:
                    ident = d.get("identifier")
                    title = d.get("title", "")
                    out.write(f"  ID: {ident} | Title: {title}\n")
                    # fetch files
                    f_url = f"https://archive.org/metadata/{ident}/files"
                    try:
                        with urllib.request.urlopen(urllib.request.Request(f_url, headers={"User-Agent": "Mozilla/5.0"}), timeout=5) as fr:
                            fdata = json.loads(fr.read().decode("utf-8"))
                            for f in fdata.get("result", []):
                                fn = f.get("name", "")
                                if fn.lower().endswith(".mp3"):
                                    out.write(f"    -> {fn} ({f.get('size')} bytes)\n")
                    except Exception as fe:
                        out.write(f"    (Files error: {fe})\n")
        except Exception as e:
            out.write(f"Error {key}: {e}\n")

print("Done searching.")
