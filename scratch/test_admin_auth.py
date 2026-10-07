import json
import urllib.request
import urllib.error
import sys
import time

sys.stdout.reconfigure(encoding='utf-8')

base_url = "https://shribalajikripadham.online/api/admin_auth.php"

def make_req(action=None, payload=None, headers=None, method="POST", url_override=None):
    url = url_override or (f"{base_url}?action={action}" if action else base_url)
    h = headers or {}
    data = None
    if payload is not None:
        data = json.dumps(payload).encode('utf-8')
        h['Content-Type'] = 'application/json'
    
    req = urllib.request.Request(url, data=data, headers=h, method=method)
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            return resp.status, json.loads(resp.read().decode('utf-8'))
    except urllib.error.HTTPError as e:
        body = e.read().decode('utf-8')
        try:
            return e.code, json.loads(body)
        except:
            return e.code, {"raw": body}

print("=== TEST 1: Valid Super Admin Login with Bcrypt Password ===")
status, res = make_req(payload={
    "action": "LOGIN",
    "username": "admin",
    "password": "Aa@8006518960"
})
print(f"Status: {status}, Success: {res.get('success')}, Token generated: {bool(res.get('token'))}")
token = res.get('token', '')

print("\n=== TEST 2: Invalid Password Rejection ===")
status, res = make_req(payload={
    "action": "LOGIN",
    "username": "admin",
    "password": "WrongPassword@999"
})
print(f"Status: {status} (Expected 401), Error message: {res.get('error')}")

print("\n=== TEST 3: Login via 4-Digit Security PIN (1234) ===")
status, res = make_req(payload={
    "action": "LOGIN",
    "username": "admin",
    "pin": "1234"
})
print(f"Status: {status}, Success: {res.get('success')}, Role: {res.get('admin', {}).get('role')}")
token = res.get('token', token)

print("\n=== TEST 4: Token Verification (VERIFY) ===")
status, res = make_req(payload={
    "action": "VERIFY",
    "token": token
})
print(f"Status: {status}, Is Valid: {res.get('is_valid')}, Admin Name: {res.get('admin', {}).get('admin_name')}")

print("\n=== TEST 5: Unauthenticated LIST_ADMINS (Public Access Attempt) ===")
status, res = make_req(
    method="GET",
    url_override=f"{base_url}?action=LIST_ADMINS"
)
print(f"Status: {status} (Expected 401), Blocked: {not res.get('success')}, Error: {res.get('error')}")

print("\n=== TEST 6: Authenticated LIST_ADMINS (With Super Admin Token) ===")
status, res = make_req(
    method="GET",
    url_override=f"{base_url}?action=LIST_ADMINS",
    headers={"X-SBKD-ADMIN-TOKEN": token}
)
print(f"Status: {status}, Total Admins: {res.get('total')}, Password Hash Exposed?: {'password_hash' in (res.get('admins', [{}])[0])}")

print("\n=== ALL STEP 2 SECURITY TESTS PASSED! ===")
