import urllib.request
import urllib.parse
import re

headers = {'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'}

def search_pagalnew(query):
    url = f'https://pagalnew.com/search.php?find={urllib.parse.quote(query)}'
    req = urllib.request.Request(url, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            html = resp.read().decode('utf-8', errors='ignore')
            links = re.findall(r'href="(https://pagalnew\.com/(?:songs|album)/[^"]+)"[^>]*>([^<]+)<', html)
            print(f'Pagalnew results for "{query}":')
            for l, t in links[:5]:
                print(f'   {t.strip()}: {l}')
    except Exception as e:
        print(f'Pagalnew err for {query}: {e}')

def search_pagalfree(query):
    url = f'https://pagalfree.com/search?q={urllib.parse.quote(query)}'
    req = urllib.request.Request(url, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            html = resp.read().decode('utf-8', errors='ignore')
            links = re.findall(r'href="(https://pagalfree\.com/music/[^"]+)"[^>]*>([^<]+)<', html)
            print(f'Pagalfree results for "{query}":')
            for l, t in links[:5]:
                print(f'   {t.strip()}: {l}')
    except Exception as e:
        print(f'Pagalfree err for {query}: {e}')

if __name__ == '__main__':
    for q in ['aaj mangalwar', 'meri naav chali', 'hanuman shabar mantra', 'laxmi ramana', 'satyanarayan aarti']:
        search_pagalnew(q)
        search_pagalfree(q)
        print('-'*40)
