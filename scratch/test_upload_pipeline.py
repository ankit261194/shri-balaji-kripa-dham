import urllib.request
import json
import os

dl_url = "https://archive.org/download/om-jay-lakshmi-mata-aarti-laxmi-maa.mp3/om-jay-lakshmi-mata-aarti-laxmi-maa.mp3.mp3"
dest_local = "scratch/temp_laxmi.mp3"

if not os.path.exists(dest_local):
    print("Downloading from Archive.org...")
    req_dl = urllib.request.Request(dl_url, headers={"User-Agent": "Mozilla/5.0"})
    with urllib.request.urlopen(req_dl, timeout=30) as r:
        with open(dest_local, "wb") as f:
            f.write(r.read())

print(f"Local file size: {os.path.getsize(dest_local)} bytes. Uploading to Hostinger...")

boundary = '----WebKitFormBoundary7MA4YWxkTrZu0gW'
with open(dest_local, 'rb') as f:
    file_bytes = f.read()

body = bytearray()
body.extend(f'--{boundary}\r\n'.encode('utf-8'))
body.extend(f'Content-Disposition: form-data; name="audio"; filename="laxmi_aarti.mp3"\r\n'.encode('utf-8'))
body.extend(b'Content-Type: audio/mpeg\r\n\r\n')
body.extend(file_bytes)
body.extend(f'\r\n--{boundary}--\r\n'.encode('utf-8'))

req = urllib.request.Request(
    'https://shribalajikripadham.online/api/upload_audio.php',
    data=bytes(body),
    headers={
        'Content-Type': f'multipart/form-data; boundary={boundary}',
        'X-SBKD-API-KEY': 'SBKD_SECURE_TOKEN_9100100251233433_V243'
    },
    method='POST'
)

with urllib.request.urlopen(req, timeout=60) as resp:
    res = json.loads(resp.read().decode('utf-8'))
    print("SUCCESS! Audio URL:", res.get("audio_url"))
