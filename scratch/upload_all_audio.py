import urllib.request
import os
import json
import sys

sys.stdout.reconfigure(encoding='utf-8')

audio_files = [
    ('hanuman_chalisa.mp3', 'hanuman_chalisa'),
    ('aarti_kije.mp3', 'hanuman_aarti'),
    ('sankatmochan.mp3', 'sankat_mochan'),
    ('bajrang_baan.mp3', 'bajrang_baan'),
    ('ram_stuti.mp3', 'ram_stuti'),
    ('balaji_aarti.mp3', 'balaji_aarti'),
    ('bhairav_aarti.mp3', 'bhairav_aarti'),
    ('pretraj_chalisa.mp3', 'pretraj_chalisa'),
    ('guru_vandana.mp3', 'guru_vandana'),
    ('ganesh_aarti.mp3', 'ganesh_aarti'),
    ('durga_aarti.mp3', 'durga_aarti'),
    ('shiv_aarti.mp3', 'shiv_aarti'),
]

url_map = {}

def upload_mp3(file_path, filename):
    print(f"Uploading {filename} ({os.path.getsize(file_path)} bytes)...")
    boundary = '----WebKitFormBoundary7MA4YWxkTrZu0gW'
    with open(file_path, 'rb') as f:
        file_bytes = f.read()

    body = bytearray()
    body.extend(f'--{boundary}\r\n'.encode('utf-8'))
    body.extend(f'Content-Disposition: form-data; name="audio"; filename="{filename}"\r\n'.encode('utf-8'))
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
        with urllib.request.urlopen(req, timeout=180) as resp:
            data = json.loads(resp.read().decode('utf-8'))
            url = data.get('audio_url')
            print(f"Success: {url}")
            return url
    except Exception as e:
        print(f"Failed to upload {filename}: {e}")
        return None

for fname, key in audio_files:
    fpath = os.path.join('backend', 'media', fname)
    if os.path.exists(fpath):
        res_url = upload_mp3(fpath, fname)
        if res_url:
            url_map[key] = res_url

with open('scratch/uploaded_audio_map.json', 'w', encoding='utf-8') as f:
    json.dump(url_map, f, indent=2, ensure_ascii=False)

print(f"\nDone! Uploaded {len(url_map)} / {len(audio_files)} audio tracks.")
