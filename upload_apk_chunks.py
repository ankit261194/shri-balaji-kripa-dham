import os
import time
import urllib.request
import json

APK_PATH = r"app/build/outputs/apk/release/app-release.apk"
BASE_URL = "https://shribalajikripadham.online/sync_apk.php"
CHUNK_SIZE = 5 * 1024 * 1024 # 5 MB per chunk

def upload_apk():
    if not os.path.exists(APK_PATH):
        print(f"Error: {APK_PATH} does not exist!")
        return False
    
    total_size = os.path.getsize(APK_PATH)
    print(f"Starting chunked upload of {APK_PATH} ({total_size / (1024*1024):.2f} MB)...")
    
    # 1. Reset any previous incomplete upload
    try:
        urllib.request.urlopen(f"{BASE_URL}?reset=1", timeout=15)
        print("Reset previous upload state.")
    except Exception as e:
        print(f"Reset warning: {e}")

    # 2. Upload chunks sequentially
    with open(APK_PATH, "rb") as f:
        chunk_idx = 0
        bytes_sent = 0
        while True:
            chunk_data = f.read(CHUNK_SIZE)
            if not chunk_data:
                break
            
            bytes_sent += len(chunk_data)
            is_last = (bytes_sent >= total_size)
            
            url = f"{BASE_URL}?chunk_upload=1&chunk={chunk_idx}&last={'1' if is_last else '0'}"
            
            retries = 3
            success = False
            while retries > 0:
                try:
                    req = urllib.request.Request(
                        url,
                        data=chunk_data,
                        headers={
                            "Content-Type": "application/octet-stream",
                            "User-Agent": "SBKD-Uploader/1.0"
                        }
                    )
                    with urllib.request.urlopen(req, timeout=45) as resp:
                        res_body = resp.read().decode('utf-8', errors='ignore')
                        print(f"Chunk {chunk_idx} ({len(chunk_data)} bytes, {bytes_sent}/{total_size} - {bytes_sent*100/total_size:.1f}%): {res_body.strip()}")
                        success = True
                        break
                except Exception as ex:
                    print(f"Retry {3 - retries + 1} for chunk {chunk_idx}: {ex}")
                    retries -= 1
                    time.sleep(2)
            
            if not success:
                print(f"Failed to upload chunk {chunk_idx} after retries!")
                return False
            
            chunk_idx += 1

    print("\nAPK Upload Finished! Syncing version.json...")
    
    # 3. Sync version.json
    try:
        ver_path = r"backend/version.json"
        if os.path.exists(ver_path):
            with open(ver_path, "r", encoding="utf-8") as vf:
                ver_json = vf.read()
            req = urllib.request.Request(
                f"{BASE_URL}?sync_version=1",
                data=ver_json.encode('utf-8'),
                headers={"Content-Type": "application/json"}
            )
            with urllib.request.urlopen(req, timeout=15) as vresp:
                print("Version sync response:", vresp.read().decode('utf-8', errors='ignore'))
    except Exception as e:
        print("Version sync error:", e)

    return True

if __name__ == "__main__":
    upload_apk()
