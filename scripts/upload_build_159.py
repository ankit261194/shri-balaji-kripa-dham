import os
import sys
import json
import urllib.request
import ssl
import time

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

BASE_URL = "https://shribalajikripadham.online"
APK_PATH = r"C:\Users\hp\.gemini\antigravity\scratch\shri_balaji_kripa_dham\app\build\outputs\apk\release\app-release.apk"
VERSION_JSON_PATH = r"C:\Users\hp\.gemini\antigravity\scratch\shri_balaji_kripa_dham\version.json"

def sync_version():
    print("Syncing version.json to Hostinger...")
    with open(VERSION_JSON_PATH, "r", encoding="utf-8") as f:
        v_data = f.read()
    
    url = f"{BASE_URL}/sync_apk.php?sync_version=1"
    req = urllib.request.Request(
        url,
        data=v_data.encode("utf-8"),
        headers={"Content-Type": "application/json"}
    )
    with urllib.request.urlopen(req, context=ctx, timeout=15) as resp:
        res = json.loads(resp.read().decode("utf-8"))
        print("Version sync result:", res)

def reset_tmp():
    print("Resetting remote tmp upload file...")
    url = f"{BASE_URL}/sync_apk.php?reset=1"
    req = urllib.request.Request(url)
    with urllib.request.urlopen(req, context=ctx, timeout=15) as resp:
        res = json.loads(resp.read().decode("utf-8"))
        print("Reset result:", res)

def upload_chunks():
    total_size = os.path.getsize(APK_PATH)
    chunk_size = 2 * 1024 * 1024 # 2MB chunks
    total_chunks = (total_size + chunk_size - 1) // chunk_size
    print(f"Uploading Build 159 APK ({total_size / (1024*1024):.2f} MB in {total_chunks} chunks)...")

    with open(APK_PATH, "rb") as f:
        for i in range(total_chunks):
            chunk = f.read(chunk_size)
            is_last = 1 if (i == total_chunks - 1) else 0
            url = f"{BASE_URL}/sync_apk.php?chunk_upload=1&chunk={i}&last={is_last}"
            
            for attempt in range(3):
                try:
                    req = urllib.request.Request(
                        url,
                        data=chunk,
                        headers={
                            "Content-Type": "application/octet-stream",
                            "Content-Length": str(len(chunk))
                        }
                    )
                    with urllib.request.urlopen(req, context=ctx, timeout=30) as resp:
                        res = resp.read().decode("utf-8")
                        if is_last:
                            print(f"\nFinal response: {res}")
                        else:
                            sys.stdout.write(f"\rUploaded chunk {i+1}/{total_chunks} ({(i+1)*100//total_chunks}%)")
                            sys.stdout.flush()
                        break
                except Exception as e:
                    print(f"\nRetry chunk {i+1} due to: {e}")
                    time.sleep(2)
            else:
                print(f"\nFailed to upload chunk {i+1} after 3 attempts.")
                sys.exit(1)

    print("\nAPK upload complete!")

def verify_cdn():
    print("Verifying CDN download URL...")
    url = f"{BASE_URL}/downloads/ShriBalajiKripaDham-release.apk"
    req = urllib.request.Request(url, method="HEAD")
    with urllib.request.urlopen(req, context=ctx, timeout=15) as resp:
        cl = resp.headers.get("Content-Length")
        print(f"CDN APK Verified! Content-Length: {cl} bytes ({int(cl)/(1024*1024):.2f} MB)")

if __name__ == "__main__":
    reset_tmp()
    upload_chunks()
    sync_version()
    verify_cdn()
