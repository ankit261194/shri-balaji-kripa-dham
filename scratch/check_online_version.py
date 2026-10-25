import sys
import urllib.request
import json

sys.stdout.reconfigure(encoding='utf-8')

req = urllib.request.Request('https://shribalajikripadham.online/version.json', headers={'User-Agent': 'Mozilla/5.0'})
try:
    with urllib.request.urlopen(req) as resp:
        data = resp.read().decode('utf-8')
        print("Live version.json content:")
        print(data)
except Exception as e:
    print("Error fetching:", e)
