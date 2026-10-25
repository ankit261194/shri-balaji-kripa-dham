import urllib.request
import json
import os

src_url = 'https://archive.org/download/y2mate.comomjaijagdishhareaartibhaktisongsvzx5jqnzd6u/y2mate.com%20-%20om_jai_jagdish_hare_aarti_bhakti_songs_Vzx5jQnzd6U.mp3'
dest_local = 'scratch/temp_jagdish.mp3'

print('Downloading Jagdish Aarti...')
req_dl = urllib.request.Request(src_url, headers={'User-Agent': 'Mozilla/5.0'})
with urllib.request.urlopen(req_dl, timeout=30) as r:
    with open(dest_local, 'wb') as f:
        f.write(r.read())

size = os.path.getsize(dest_local)
print(f'Downloaded {size} bytes. Uploading to Hostinger...')

boundary = '----WebKitFormBoundaryJagdish'
with open(dest_local, 'rb') as f:
    file_bytes = f.read()

body = bytearray()
body.extend(f'--{boundary}\r\n'.encode('utf-8'))
body.extend(b'Content-Disposition: form-data; name="audio"; filename="jagdish_aarti.mp3"\r\n')
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
    print('SUCCESS! Uploaded Jagdish Audio URL:', res.get('audio_url'))
