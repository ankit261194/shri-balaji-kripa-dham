import urllib.request, json

# We can test by POSTing to live_config.php which accepts fields
# But let's check what upload_photo.php does
url = 'https://shribalajikripadham.online/api/upload_photo.php'
boundary = '==Boundary_Check=='

lines = [
    f'--{boundary}',
    'Content-Disposition: form-data; name="photo_type"',
    '',
    'guruji',
    f'--{boundary}',
    'Content-Disposition: form-data; name="photo"; filename="check.jpg"',
    'Content-Type: image/jpeg',
    '',
    'fake_image',
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
    res = json.loads(resp.read().decode('utf-8'))
    print('Photo URL:', res.get('photo_url'))

# Now check if DB has this photo_url
req2 = urllib.request.Request('https://shribalajikripadham.online/api/live_config.php?nocache=' + str(__import__('time').time()), headers={'User-Agent': 'Test'})
with urllib.request.urlopen(req2) as resp2:
    cfg = json.loads(resp2.read().decode('utf-8')).get('config', {})
    print('DB guruji_photo_url:', cfg.get('guruji_photo_url'))
    print('MATCH:', res.get('photo_url') == cfg.get('guruji_photo_url'))
