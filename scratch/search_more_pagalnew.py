import urllib.request
import urllib.parse
import re

def search_pagalnew(term):
    url = f"https://pagalnew.com/search.php?find={urllib.parse.quote(term)}"
    req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'})
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            html = resp.read().decode('utf-8', errors='ignore')
            links = re.findall(r'href="([^"]*/songs/[^"]+)"', html)
            return links
    except Exception as e:
        return [str(e)]

terms = [
    'khatu shyam aarti',
    'shyam aarti',
    'satyanarayan aarti',
    'satyanarayan',
    'jai lakshmi ramana',
    'vindheshwari',
    'vindhyavasini',
    'pretraj',
    'bajrang bali',
    'meri naav chali',
    'aaj mangalwar hai'
]

for t in terms:
    print(f"=== {t} ===")
    print(search_pagalnew(t))
