import os
import sys
import json
import urllib.request
import ssl
import time

sys.stdout.reconfigure(encoding="utf-8")

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

BASE_URL = "https://shribalajikripadham.online"
APK_PATH = r"C:\Users\hp\.gemini\antigravity\scratch\shri_balaji_kripa_dham\app\build\outputs\apk\release\app-release.apk"
VERSION_JSON_PATH = r"C:\Users\hp\.gemini\antigravity\scratch\shri_balaji_kripa_dham\version.json"

def reset_tmp():
    print("Resetting remote tmp upload file...")
    url = f"{BASE_URL}/sync_apk.php?reset=1"
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
    with urllib.request.urlopen(req, context=ctx, timeout=15) as resp:
        res = json.loads(resp.read().decode("utf-8"))
        print("Reset result:", res)
        assert res.get("success") is True, f"Reset failed: {res}"

def upload_chunks():
    total_size = os.path.getsize(APK_PATH)
    chunk_size = 2 * 1024 * 1024 # 2MB chunks
    total_chunks = (total_size + chunk_size - 1) // chunk_size
    print(f"Uploading Build 168 APK ({total_size / (1024*1024):.2f} MB in {total_chunks} chunks)...")

    with open(APK_PATH, "rb") as f:
        for i in range(total_chunks):
            chunk = f.read(chunk_size)
            is_last = 1 if (i == total_chunks - 1) else 0
            url = f"{BASE_URL}/sync_apk.php?chunk_upload=1&chunk={i}&last={is_last}"
            
            # Use longer timeout for last chunk where server renames and copies
            timeout_sec = 60 if is_last else 35
            
            uploaded = False
            for attempt in range(4):
                try:
                    req = urllib.request.Request(
                        url,
                        data=chunk,
                        headers={
                            "Content-Type": "application/octet-stream",
                            "Content-Length": str(len(chunk)),
                            "User-Agent": "Mozilla/5.0"
                        }
                    )
                    with urllib.request.urlopen(req, context=ctx, timeout=timeout_sec) as resp:
                        res = json.loads(resp.read().decode("utf-8"))
                        if res.get("success") is True:
                            if is_last:
                                print(f"==> FINAL Chunk {i+1}/{total_chunks} complete! Result: {res}")
                            else:
                                print(f"Chunk {i+1}/{total_chunks} uploaded ({res.get('bytes_so_far', 0)} bytes so far)")
                            uploaded = True
                            break
                        else:
                            print(f"Chunk {i+1} attempt {attempt+1} server error: {res}")
                except Exception as e:
                    print(f"Chunk {i+1} attempt {attempt+1} network error: {e}")
                    time.sleep(2)
            
            if not uploaded:
                raise RuntimeError(f"Failed to upload chunk {i+1}/{total_chunks} after 4 attempts")

def verify_live_status():
    print("\n--- Verifying Server Status ---")
    status_url = f"{BASE_URL}/sync_apk.php?status=1"
    req = urllib.request.Request(status_url, headers={"User-Agent": "Mozilla/5.0"})
    with urllib.request.urlopen(req, context=ctx, timeout=15) as resp:
        res = json.loads(resp.read().decode("utf-8"))
        print("Server status:")
        print(json.dumps(res, indent=2))
        assert res.get("target_release_exists") is True, "Target release APK does not exist on server!"
        assert res.get("target_release_size") == os.path.getsize(APK_PATH), "Target release APK size mismatch!"

def verify_download():
    print("\n--- Testing Live Download Endpoint ---")
    dl_url = f"{BASE_URL}/download.php"
    req = urllib.request.Request(dl_url, headers={"User-Agent": "Mozilla/5.0 (Android; Mobile)"})
    with urllib.request.urlopen(req, context=ctx, timeout=20) as resp:
        print("Download HTTP Status:", resp.status)
        print("Content-Type:", resp.headers.get("Content-Type"))
        print("Content-Length:", resp.headers.get("Content-Length"))
        print("Content-Disposition:", resp.headers.get("Content-Disposition"))
        assert resp.status == 200, f"Expected 200 OK, got {resp.status}"
        assert resp.headers.get("Content-Type") == "application/vnd.android.package-archive", "Wrong Content-Type"
        assert int(resp.headers.get("Content-Length")) == os.path.getsize(APK_PATH), "Size mismatch"
        header_bytes = resp.read(1024)
        print(f"First 1KB downloaded successfully ({len(header_bytes)} bytes)")
    
    print("\n--- Testing Direct Downloads APK URL ---")
    direct_url = f"{BASE_URL}/downloads/ShriBalajiKripaDham-release.apk"
    head_req = urllib.request.Request(direct_url, method="HEAD", headers={"User-Agent": "Mozilla/5.0"})
    with urllib.request.urlopen(head_req, context=ctx, timeout=15) as resp:
        print("Direct URL Status:", resp.status)
        print("Direct Content-Length:", resp.headers.get("Content-Length"))
        print("Direct Content-Type:", resp.headers.get("Content-Type"))
        assert resp.status == 200, f"Expected 200 OK, got {resp.status}"
        assert int(resp.headers.get("Content-Length")) == os.path.getsize(APK_PATH), "Size mismatch"

if __name__ == "__main__":
    reset_tmp()
    upload_chunks()
    verify_live_status()
    verify_download()
    print("\n=======================================================")
    print("ALL VERIFICATIONS PASSED 100%! APP DOWNLOAD IS FULLY FIXED!")
    print("=======================================================")
