import urllib.request
import os
import json
import sys

sys.stdout.reconfigure(encoding='utf-8')

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
        with urllib.request.urlopen(req, timeout=120) as resp:
            data = json.loads(resp.read().decode('utf-8'))
            print("Success:", data.get('audio_url'))
            return data.get('audio_url')
    except Exception as e:
        print(f"Failed to upload {filename}:", e)
        return None

# Test guru_vandana (2.4MB) and bajrang_baan (3.3MB)
upload_mp3('backend/media/guru_vandana.mp3', 'guru_vandana.mp3')
upload_mp3('backend/media/bajrang_baan.mp3', 'bajrang_baan.mp3')
