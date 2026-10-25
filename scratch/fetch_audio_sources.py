import urllib.request
import urllib.parse
import json

queries = [
    "Om Jai Jagdish Hare",
    "Om Jai Lakshmi Mata",
    "Aarti Kunj Bihari Ki",
    "Aarti Shri Ramayan Ji Ki",
    "Khatu Shyam Aarti",
    "Satyanarayan Aarti",
    "Vindhweshwari Chalisa",
    "Durga Saptashati",
    "Pretraj Sarkar",
    "Meri Naav Chali Bajrang Bali",
    "Aaj Mangalwar Hai"
]

def search_and_list(q):
    url = f"https://archive.org/advancedsearch.php?q={urllib.parse.quote(q)}+AND+mediatype:audio&fl[]=identifier,title&rows=3&output=json"
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
    try:
        with urllib.request.urlopen(req, timeout=10) as r:
            data = json.loads(r.read().decode("utf-8"))
            docs = data.get("response", {}).get("docs", [])
            print(f"\n=== Query: {q} ({len(docs)} found) ===")
            for d in docs:
                ident = d.get("identifier")
                title = d.get("title", "")
                print(f"  Identifier: {ident} | Title: {title}")
                # Get mp3 files
                meta_url = f"https://archive.org/metadata/{ident}/files"
                m_req = urllib.request.Request(meta_url, headers={"User-Agent": "Mozilla/5.0"})
                try:
                    with urllib.request.urlopen(m_req, timeout=5) as mr:
                        m_data = json.loads(mr.read().decode("utf-8"))
                        for f in m_data.get("result", []):
                            fname = f.get("name", "")
                            if fname.lower().endswith(".mp3"):
                                print(f"    -> MP3: {fname} ({f.get('size')} bytes)")
                except Exception as me:
                    print(f"    (Files error: {me})")
    except Exception as e:
        print(f"Error {q}: {e}")

for q in queries:
    search_and_list(q)
