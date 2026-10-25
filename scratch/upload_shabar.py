import urllib.request
import json
import os

dest_local = 'scratch/test_shabar.mp3'
size = os.path.getsize(dest_local)
print(f'Uploading {size} bytes to Hostinger...')

boundary = '----WebKitFormBoundaryShabar'
with open(dest_local, 'rb') as f:
    file_bytes = f.read()

body = bytearray()
body.extend(f'--{boundary}\r\n'.encode('utf-8'))
body.extend(b'Content-Disposition: form-data; name="audio"; filename="veer_shabar_mantra.mp3"\r\n')
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
    print('SUCCESS! Uploaded Shabar Audio URL:', res.get('audio_url'))
