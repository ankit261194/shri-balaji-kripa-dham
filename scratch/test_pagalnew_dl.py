import urllib.request

url = 'https://pagalnew.com/128-downloads/32014'
req = urllib.request.Request(
    url,
    headers={
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
        'Referer': 'https://pagalnew.com/songs/aarti-shri-ramayan-ji-ki-aarti-vol-3.html'
    }
)
try:
    with urllib.request.urlopen(req, timeout=15) as resp:
        print("Status:", resp.status)
        print("Content-Type:", resp.headers.get("Content-Type"))
        print("Content-Length:", resp.headers.get("Content-Length"))
        chunk = resp.read(1024)
        print("First 10 bytes:", chunk[:10])
except Exception as e:
    print("Error:", e)
