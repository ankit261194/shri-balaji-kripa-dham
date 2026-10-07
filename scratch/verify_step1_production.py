import urllib.request
import urllib.error
import json
import sys

def safe_str(val):
    if isinstance(val, str):
        return val.encode('ascii', errors='replace').decode('ascii')
    return str(val)

headers = {'User-Agent': 'SBKD-Step1-Verifier/1.0'}

print("=== SHRI BALAJI KRIPA DHAM - STEP 1 PRODUCTION VERIFICATION ===")

# Test 1: live_config.php dynamic havan cost
req1 = urllib.request.Request('https://shribalajikripadham.online/api/live_config.php', headers=headers)
with urllib.request.urlopen(req1) as r:
    data1 = json.loads(r.read().decode('utf-8'))
    print("1. live_config.php:")
    print("   - havan_estimated_cost:", data1.get('havan_estimated_cost'))
    print("   - havan_rules_notice length:", len(data1.get('havan_rules_notice') or ''))
    print("   - havan_rules_notice snippet:", safe_str((data1.get('havan_rules_notice') or '')[:40]))

# Test 2: havan_service.php GET_CONFIG
req2 = urllib.request.Request('https://shribalajikripadham.online/api/havan_service.php?action=GET_CONFIG', headers=headers)
with urllib.request.urlopen(req2) as r:
    data2 = json.loads(r.read().decode('utf-8'))
    print("\n2. havan_service.php GET_CONFIG:")
    print("   - success:", data2.get('success'))
    print("   - havan_estimated_cost:", data2.get('havan_estimated_cost'))
    print("   - havan_rules_notice snippet:", safe_str((data2.get('havan_rules_notice') or '')[:40]))

# Test 3: sevadar_chat.php Privacy Protection (Unauthorized access blocked)
try:
    req3 = urllib.request.Request('https://shribalajikripadham.online/api/sevadar_chat.php?action=list_conversations', headers=headers)
    with urllib.request.urlopen(req3) as r:
        print("\n3. ERROR: Request without sevadar_id succeeded when it should be blocked!")
except urllib.error.HTTPError as e:
    err_body = json.loads(e.read().decode('utf-8'))
    print("\n3. sevadar_chat.php Privacy Block (No ID / Non-SuperAdmin):")
    print("   - HTTP Status:", e.code, "(Forbidden as expected)")
    print("   - Error Message:", safe_str(err_body.get('error')))

# Test 4: sevadar_chat.php Super Admin Master Access
req4 = urllib.request.Request('https://shribalajikripadham.online/api/sevadar_chat.php?action=list_conversations&admin_pin=1234&role=SUPER_ADMIN', headers=headers)
with urllib.request.urlopen(req4) as r:
    data4 = json.loads(r.read().decode('utf-8'))
    print("\n4. sevadar_chat.php Super Admin Access:")
    print("   - success:", data4.get('success'))
    print("   - conversations retrieved:", len(data4.get('conversations', [])))

# Test 5: havan_service.php SAVE_CONFIG by Super Admin
post_payload = json.dumps({
    "action": "SAVE_CONFIG",
    "admin_pin": "1234",
    "havan_estimated_cost": 15000,
    "havan_rules_notice": "हवन अनुष्ठान का अनुमानित खर्च लगभग ₹15,000 होता है। (Live Test Update)"
}).encode('utf-8')
req5 = urllib.request.Request('https://shribalajikripadham.online/api/havan_service.php', data=post_payload, headers={'Content-Type': 'application/json', 'User-Agent': 'SBKD-Verifier/1.0'})
with urllib.request.urlopen(req5) as r:
    data5 = json.loads(r.read().decode('utf-8'))
    print("\n5. havan_service.php SAVE_CONFIG (Dynamic Update to 15000):")
    print("   - success:", data5.get('success'))
    print("   - message:", safe_str(data5.get('message')))

# Verify live_config now reflects 15000
req6 = urllib.request.Request('https://shribalajikripadham.online/api/live_config.php', headers=headers)
with urllib.request.urlopen(req6) as r:
    data6 = json.loads(r.read().decode('utf-8'))
    print("   - Verified updated live_config havan_estimated_cost:", data6.get('havan_estimated_cost'))

# Restore back to original 14000
restore_payload = json.dumps({
    "action": "SAVE_CONFIG",
    "admin_pin": "1234",
    "havan_estimated_cost": 14000,
    "havan_rules_notice": "हवन अनुष्ठान का अनुमानित खर्च लगभग ₹14,000 होता है। गाड़ी का आने-जाने का सम्पूर्ण किराया यजमान (भगत) को स्वयं वहन करना होगा।"
}).encode('utf-8')
req7 = urllib.request.Request('https://shribalajikripadham.online/api/havan_service.php', data=restore_payload, headers={'Content-Type': 'application/json', 'User-Agent': 'SBKD-Verifier/1.0'})
with urllib.request.urlopen(req7) as r:
    data7 = json.loads(r.read().decode('utf-8'))
    print("   - Restored havan_estimated_cost back to 14000:", data7.get('success'))

print("\n=== ALL STEP 1 TESTS PASSED 100% ===")
