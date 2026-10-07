import urllib.request, json

url = 'https://shribalajikripadham.online/api/upload_photo.php'
boundary = '==Boundary_Test456=='

lines = [
    f'--{boundary}',
    'Content-Disposition: form-data; name="photo_type"',
    '',
    'guruji',
    f'--{boundary}',
    'Content-Disposition: form-data; name="photo"; filename="guruji_test.jpg"',
    'Content-Type: image/jpeg',
    '',
    'fake_image_bytes',
    f'--{boundary}--',
    ''
]
body = '\r\n'.join(lines).encode('utf-8')

req = urllib.request.Request(
    url,
    data=body,
    headers={
        'Content-Type': f'multipart/form-data; boundary={boundary}',
        'X-SBKD-API-KEY': 'SBKD_SECURE_TOKEN_9100100251233433_V243',
        'User-Agent': 'Test'
    }
)

with urllib.request.urlopen(req) as resp:
    print('Upload resp:', resp.read().decode('utf-8', errors='ignore').encode('ascii', errors='replace').decode('ascii'))

# Now check if guruji_photo_url changed in live_config.php
req2 = urllib.request.Request('https://shribalajikripadham.online/api/live_config.php?nocache=1', headers={'User-Agent': 'Test'})
with urllib.request.urlopen(req2) as resp2:
    data = json.loads(resp2.read().decode('utf-8'))
    print('Current guruji_photo_url:', data.get('config', {}).get('guruji_photo_url'))
