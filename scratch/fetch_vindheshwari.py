import urllib.request
import re

url = 'https://pagalnew.com/songs/shree-vindheshwari-chalisa-durga-chalisa-durga-kawach.html'
req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'})
with urllib.request.urlopen(req) as resp:
    html = resp.read().decode('utf-8', errors='ignore')
    all_dls = re.findall(r'href="([^"]*download[^"]*)"', html, re.I)
    print('Vindheshwari Download links:', all_dls)
