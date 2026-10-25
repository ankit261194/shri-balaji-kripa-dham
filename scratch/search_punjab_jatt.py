import urllib.request
import urllib.parse
import re

def search_djpunjab(term):
    url = f"https://djpunjab.is/search?q={urllib.parse.quote(term)}"
    req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'})
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            html = resp.read().decode('utf-8', errors='ignore')
            links = re.findall(r'href="([^"]*(?:song|track)[^"]*)"', html, re.I)
            if not links:
                links = re.findall(r'href="([^"]*\.html)"', html)
            return links[:5]
    except Exception as e:
        return [str(e)]

def search_mrjatt(term):
    url = f"https://mr-jatt.im/search/{urllib.parse.quote(term)}"
    req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'})
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            html = resp.read().decode('utf-8', errors='ignore')
            links = re.findall(r'href="([^"]*(?:song|track)[^"]*)"', html, re.I)
            if not links:
                links = re.findall(r'href="([^"]*\.html)"', html)
            return links[:5]
    except Exception as e:
        return [str(e)]

terms = ['khatu shyam aarti', 'satyanarayan aarti', 'jai lakshmi ramana']
for t in terms:
    print(f"djpunjab {t}:", search_djpunjab(t))
    print(f"mrjatt {t}:", search_mrjatt(t))
