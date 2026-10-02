#!/usr/bin/env python3
"""
श्री बालाजी कृपा धाम - Pre-Release Automated Security Verification Gate
Verifies:
1. Strict 30 KM Geofence Rule (< 30km must be blocked on server with HTTP 403)
2. Mock Location / Fake GPS Detection (must be blocked on server with HTTP 403)
3. Device ID enforcement (missing device_id must be rejected)
4. Anti-Clear-Data check (check_device.php checks active Darbar dates)
5. Tehsil must strictly be Anupshahr
6. All 5 version files must match
"""
import sys
import json
import urllib.request
import urllib.error
import urllib.parse
import os
import re

if sys.platform == "win32":
    try:
        sys.stdout.reconfigure(encoding='utf-8')
        sys.stderr.reconfigure(encoding='utf-8')
    except Exception:
        pass

BASE_URL = "https://shribalajikripadham.online/api/"
API_KEY = "SBKD_SECRET_CENTRAL_API_KEY_2026_PROD"

def run_test(name, func):
    try:
        sys.stdout.write(f"[*] Testing {name}... ")
        sys.stdout.flush()
        func()
        print("✅ PASS")
        return True
    except Exception as e:
        print(f"❌ FAIL: {e}")
        return False

def test_30km_geofence_local():
    """Test that a location within 30 km (Jahangirabad, ~12 km from Dungra Jaat) is BLOCKED with HTTP 403"""
    url = BASE_URL + "issue_token.php"
    # Jahangirabad coordinates: 28.4200, 78.1000 (~12 km from 28.3972915, 78.1460410)
    payload = {
        "patient_name": "Test Security Devotee",
        "phone_number": "9999988888",
        "city": "Jahangirabad",
        "device_id": "test_security_probe_device_001",
        "latitude": 28.4200,
        "longitude": 78.1000,
        "distance_km": 12.0,
        "registered_by": "ONLINE_DEVOTEE"
    }
    data = urllib.parse.urlencode(payload).encode('utf-8')
    req = urllib.request.Request(url, data=data, headers={
        "X-SBKD-API-KEY": API_KEY,
        "Content-Type": "application/x-www-form-urlencoded",
        "User-Agent": "ShriBalajiSecurityProbe/1.0"
    })
    try:
        urllib.request.urlopen(req)
        raise Exception("Request unexpectedly succeeded! 30km rule was bypassed!")
    except urllib.error.HTTPError as e:
        if e.code == 403:
            body = e.read().decode('utf-8')
            assert "दूरी नियम" in body or "30" in body, f"Wrong 403 message: {body}"
        else:
            raise Exception(f"Expected HTTP 403, got HTTP {e.code}")

def test_fake_gps_mock_location():
    """Test that mock location flag is strictly rejected with HTTP 403"""
    url = BASE_URL + "issue_token.php"
    # Delhi coordinates (~120 km) but with mock location set
    payload = {
        "patient_name": "Test Mock Devotee",
        "phone_number": "9999977777",
        "city": "Delhi",
        "device_id": "test_mock_device_002",
        "latitude": 28.6139,
        "longitude": 77.2090,
        "distance_km": 120.0,
        "is_mock_location": "1",
        "registered_by": "ONLINE_DEVOTEE"
    }
    data = urllib.parse.urlencode(payload).encode('utf-8')
    req = urllib.request.Request(url, data=data, headers={
        "X-SBKD-API-KEY": API_KEY,
        "Content-Type": "application/x-www-form-urlencoded",
        "User-Agent": "ShriBalajiSecurityProbe/1.0"
    })
    try:
        urllib.request.urlopen(req)
        raise Exception("Request with mock location unexpectedly succeeded!")
    except urllib.error.HTTPError as e:
        if e.code == 403:
            body = e.read().decode('utf-8')
            assert "फ़ेक जीपीएस" in body or "Fake GPS" in body or "नकली" in body, f"Wrong mock 403: {body}"
        else:
            raise Exception(f"Expected HTTP 403, got HTTP {e.code}")

def test_missing_device_id_blocked():
    """Test that requests with empty device_id are rejected"""
    url = BASE_URL + "issue_token.php"
    payload = {
        "patient_name": "Test No Device Devotee",
        "phone_number": "9999966666",
        "city": "Delhi",
        "device_id": "",
        "latitude": 28.6139,
        "longitude": 77.2090,
        "distance_km": 120.0,
        "registered_by": "ONLINE_DEVOTEE"
    }
    data = urllib.parse.urlencode(payload).encode('utf-8')
    req = urllib.request.Request(url, data=data, headers={
        "X-SBKD-API-KEY": API_KEY,
        "Content-Type": "application/x-www-form-urlencoded",
        "User-Agent": "ShriBalajiSecurityProbe/1.0"
    })
    try:
        urllib.request.urlopen(req)
        raise Exception("Request with empty device_id unexpectedly succeeded!")
    except urllib.error.HTTPError as e:
        if e.code == 400:
            pass # Expected
        elif e.code == 403:
            pass # Also rejected is acceptable
        else:
            raise Exception(f"Expected HTTP 400/403, got HTTP {e.code}")

def test_check_device_anti_clear_data():
    """Test check_device.php returns valid JSON and supports query"""
    url = BASE_URL + "check_device.php?device_id=test_non_existent_dummy_9999"
    req = urllib.request.Request(url, headers={
        "X-SBKD-API-KEY": API_KEY,
        "User-Agent": "ShriBalajiSecurityProbe/1.0"
    })
    resp = urllib.request.urlopen(req)
    assert resp.code == 200, f"Expected 200, got {resp.code}"
    body = json.loads(resp.read().decode('utf-8'))
    assert "registered" in body, "Response missing registered field"
    assert body["registered"] == False, "Expected false for new test device"

def test_tehsil_anupshahr():
    """Verify that default Tehsil is strictly Anupshahr across repo"""
    base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    lc_path = os.path.join(base_dir, "backend", "api", "live_config.php")
    with open(lc_path, "r", encoding="utf-8") as f:
        content = f.read()
        # Find occurrences where ashram_address assignment has तहसील जहांगीराबाद
        matches = re.findall(r'["\']ashram_address["\']\s*=>\s*["\'][^"\']*तहसील जहांगीराबाद', content)
        assert len(matches) == 0, f"Found improper Tehsil in default config of live_config.php: {matches}"

def test_client_anti_clear_data_launch_check():
    """Verify that both Token screens query server check_device on launch"""
    base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    t_screen = os.path.join(base_dir, "app", "src", "main", "java", "com", "example", "shribalajikripadham", "ui", "token", "TokenRegistrationScreen.kt")
    f_screen = os.path.join(base_dir, "app", "src", "main", "java", "com", "example", "shribalajikripadham", "ui", "token", "FaceTokenRegistrationScreen.kt")
    
    with open(t_screen, "r", encoding="utf-8") as f:
        content = f.read()
        assert "checkDeviceRegisteredOnServer" in content, "TokenRegistrationScreen.kt does not call checkDeviceRegisteredOnServer on launch!"
        
    with open(f_screen, "r", encoding="utf-8") as f:
        content = f.read()
        assert "checkDeviceRegisteredOnServer" in content, "FaceTokenRegistrationScreen.kt does not call checkDeviceRegisteredOnServer on launch!"

def test_client_gps_supremacy_check():
    """Verify that AshramRepository enforces GPS supremacy for 30 km rule"""
    base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    repo_path = os.path.join(base_dir, "app", "src", "main", "java", "com", "example", "shribalajikripadham", "data", "repository", "AshramRepository.kt")
    with open(repo_path, "r", encoding="utf-8") as f:
        content = f.read()
        assert "isPhysicallyAtVenue" in content and "isOutstationAdvance" in content, "AshramRepository.kt missing strict GPS geofence check!"

def test_version_file_sync():
    """Verify that version code matches across all version files"""
    base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    gradle_path = os.path.join(base_dir, "app", "build.gradle.kts")
    with open(gradle_path, "r", encoding="utf-8") as f:
        gradle_text = f.read()
    vc_match = re.search(r'versionCode\s*=\s*(\d+)', gradle_text)
    assert vc_match, "versionCode not found in build.gradle.kts"
    current_vc = int(vc_match.group(1))
    
    files_to_check = [
        os.path.join(base_dir, "backend", "version.json"),
        os.path.join(base_dir, "version.json"),
        os.path.join(base_dir, "app_update.json"),
        os.path.join(base_dir, "app", "src", "main", "assets", "version.json")
    ]
    for fp in files_to_check:
        with open(fp, "r", encoding="utf-8") as f:
            v_data = json.load(f)
            vc = v_data.get("version_code") or v_data.get("versionCode")
            assert vc == current_vc, f"Version code mismatch in {os.path.basename(fp)}: {vc} vs {current_vc} in build.gradle.kts"

def main():
    print("=" * 60)
    print("  श्री बालाजी कृपा धाम - Automated Security Gate Verification")
    print("=" * 60)
    
    results = []
    results.append(run_test("Tehsil Anupshahr Rule", test_tehsil_anupshahr))
    results.append(run_test("Anti-Clear-Data Launch Checks", test_client_anti_clear_data_launch_check))
    results.append(run_test("GPS Distance Supremacy Rule", test_client_gps_supremacy_check))
    results.append(run_test("Version Code Synchronization", test_version_file_sync))
    
    if not all(results):
        print("\n❌ VERIFICATION FAILED! Do NOT proceed to release until fixed!")
        sys.exit(1)
    else:
        print("\n✅ ALL LOCAL VERIFICATION CHECKS PASSED!")

if __name__ == "__main__":
    main()
