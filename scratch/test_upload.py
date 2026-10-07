import urllib.request

url = 'https://shribalajikripadham.online/api/upload_photo.php'
boundary = '==Boundary_Test123=='

lines = [
    f'--{boundary}',
    'Content-Disposition: form-data; name="photo_type"',
    '',
    'darshan',
    f'--{boundary}',
    'Content-Disposition: form-data; name="photo"; filename="test.jpg"',
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

try:
    with urllib.request.urlopen(req) as resp:
        print('Status:', resp.status)
        raw = resp.read().decode('utf-8', errors='ignore')
        print('Resp:', raw.encode('ascii', errors='replace').decode('ascii'))
except Exception as e:
    print('Error:', e)
