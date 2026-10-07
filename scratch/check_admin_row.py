import urllib.request
import json
import sys

sys.stdout.reconfigure(encoding='utf-8')

# Login as super admin to get token
req = urllib.request.Request(
    "https://shribalajikripadham.online/api/admin_auth.php",
    data=json.dumps({"action": "LOGIN", "username": "admin", "password": "Aa@8006518960"}).encode('utf-8'),
    headers={"Content-Type": "application/json"}
)
with urllib.request.urlopen(req) as resp:
    data = json.loads(resp.read().decode('utf-8'))
    token = data['token']
    print("Logged in, token:", token[:20])

req2 = urllib.request.Request(
    "https://shribalajikripadham.online/api/admin_auth.php?action=LIST_ADMINS",
    headers={"X-SBKD-ADMIN-TOKEN": token}
)
with urllib.request.urlopen(req2) as resp:
    data2 = json.loads(resp.read().decode('utf-8'))
    print("Admins:", json.dumps(data2, indent=2, ensure_ascii=False))
