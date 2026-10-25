import urllib.request
import re

url = "https://pagalfree.com/"
req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'})
with urllib.request.urlopen(req, timeout=10) as resp:
    html = resp.read().decode('utf-8', errors='ignore')
    form = re.findall(r'<form[^>]*action="([^"]*)"[^>]*>', html)
    print("Forms:", form)
    inputs = re.findall(r'<input[^>]*name="([^"]*)"[^>]*>', html)
    print("Inputs:", inputs)
