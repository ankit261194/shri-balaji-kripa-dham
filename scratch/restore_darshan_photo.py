import urllib.request
import json
import sys

sys.stdout.reconfigure(encoding='utf-8')

with open('app/src/main/res/drawable/img_balaji_darshan.jpg', 'rb') as f:
    img_data = f.read()

boundary = '==DarshanBoundary123=='
part1 = (
    f'--{boundary}\r\n'
    'Content-Disposition: form-data; name="photo_type"\r\n\r\n'
    'darshan\r\n'
    f'--{boundary}\r\n'
    'Content-Disposition: form-data; name="photo"; filename="img_balaji_darshan.jpg"\r\n'
    'Content-Type: image/jpeg\r\n\r\n'
).encode('utf-8')
part2 = f'\r\n--{boundary}--\r\n'.encode('utf-8')
body = part1 + img_data + part2

req = urllib.request.Request(
    'https://shribalajikripadham.online/api/upload_photo.php',
    data=body,
    headers={
        'Content-Type': f'multipart/form-data; boundary={boundary}',
        'X-SBKD-API-KEY': 'SBKD_SECURE_TOKEN_9100100251233433_V243',
        'User-Agent': 'BalajiDarshanRestorer'
    }
)

with urllib.request.urlopen(req) as resp:
    res = json.loads(resp.read().decode('utf-8', errors='ignore'))
    print('Upload response:', json.dumps(res, indent=2, ensure_ascii=False))

# Now test downloading media/balaji_darshan_today.jpg
test_url = 'https://shribalajikripadham.online/media/balaji_darshan_today.jpg?t=' + str(__import__('time').time())
test_req = urllib.request.Request(test_url, headers={'User-Agent': 'DarshanTester'})
with urllib.request.urlopen(test_req) as tresp:
    data = tresp.read()
    print(f'Downloaded balaji_darshan_today.jpg: status={tresp.status}, size={len(data)} bytes')
