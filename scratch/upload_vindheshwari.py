import urllib.request
import json
import os

url = 'https://pagalnew.com/128-downloads/32038'
dest_local = 'scratch/temp_vindheshwari.mp3'

print('Downloading Shree Vindheshwari Chalisa...')
req = urllib.request.Request(
    url,
    headers={
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
        'Referer': 'https://pagalnew.com/songs/shree-vindheshwari-chalisa-durga-chalisa-durga-kawach.html'
    }
)

with urllib.request.urlopen(req, timeout=30) as r:
    with open(dest_local, 'wb') as f:
        f.write(r.read())

size = os.path.getsize(dest_local)
print(f'Downloaded {size} bytes. Uploading to Hostinger...')

boundary = '----WebKitFormBoundaryVindheshwari'
with open(dest_local, 'rb') as f:
    file_bytes = f.read()

body = bytearray()
body.extend(f'--{boundary}\r\n'.encode('utf-8'))
body.extend(b'Content-Disposition: form-data; name="audio"; filename="vindhweshwari_chalisa.mp3"\r\n')
body.extend(b'Content-Type: audio/mpeg\r\n\r\n')
body.extend(file_bytes)
body.extend(f'\r\n--{boundary}--\r\n'.encode('utf-8'))

req_up = urllib.request.Request(
    'https://shribalajikripadham.online/api/upload_audio.php',
    data=bytes(body),
    headers={
        'Content-Type': f'multipart/form-data; boundary={boundary}',
        'X-SBKD-API-KEY': 'SBKD_SECURE_TOKEN_9100100251233433_V243'
    },
    method='POST'
)

with urllib.request.urlopen(req_up, timeout=60) as resp:
    res = json.loads(resp.read().decode('utf-8'))
    print('SUCCESS! Uploaded Vindheshwari Audio URL:', res.get('audio_url'))
