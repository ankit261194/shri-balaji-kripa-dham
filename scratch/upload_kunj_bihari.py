import urllib.request
import urllib.parse
import json
import os

ident = "aarti-kunj-bihari-ki-krishna-ji-ki-aarti-anuradha-paudwal-256k"
meta_url = f"https://archive.org/metadata/{ident}/files"

with urllib.request.urlopen(meta_url, timeout=10) as r:
    data = json.loads(r.read().decode('utf-8'))
    mp3_name = None
    for f in data.get('result', []):
        fn = f.get('name', '')
        if fn.endswith('.mp3'):
            mp3_name = fn
            break


if mp3_name:
    dl_url = f"https://archive.org/download/{ident}/{urllib.parse.quote(mp3_name)}"
    dest = "scratch/temp_kunj_bihari.mp3"
    print("Downloading...")
    with urllib.request.urlopen(urllib.request.Request(dl_url, headers={"User-Agent": "Mozilla/5.0"}), timeout=60) as r:
        with open(dest, "wb") as out:
            out.write(r.read())
    print("Downloaded size:", os.path.getsize(dest))

    # Upload to Hostinger
    boundary = '----WebKitFormBoundary7MA4YWxkTrZu0gW'
    with open(dest, 'rb') as f:
        file_bytes = f.read()

    body = bytearray()
    body.extend(f'--{boundary}\r\n'.encode('utf-8'))
    body.extend(f'Content-Disposition: form-data; name="audio"; filename="kunj_bihari_aarti.mp3"\r\n'.encode('utf-8'))
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
        print("Kunj Bihari Audio URL:", res.get("audio_url"))
