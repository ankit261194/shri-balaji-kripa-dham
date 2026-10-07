import urllib.request
import os

file_path = 'backend/media/shiv_aarti.mp3'
boundary = '----WebKitFormBoundary7MA4YWxkTrZu0gW'
with open(file_path, 'rb') as f:
    file_bytes = f.read()

body = bytearray()
body.extend(f'--{boundary}\r\n'.encode('utf-8'))
body.extend(f'Content-Disposition: form-data; name="audio"; filename="shiv_aarti.mp3"\r\n'.encode('utf-8'))
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

try:
    with urllib.request.urlopen(req) as resp:
        print('Upload response:', resp.read().decode('utf-8'))
except Exception as e:
    print('Error:', e)
