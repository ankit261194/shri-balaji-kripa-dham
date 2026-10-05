import os
import sys
import time
import urllib.request
import json

APK_PATH = r"C:\Users\hp\Downloads\abc\Ankit_EleveX_v5.9.34_Final.apk"
BASE_URL = "https://shribalajikripadham.online/sync_elevex_apk.php"
VERSION = "5.9.34"
CHUNK_SIZE = 3 * 1024 * 1024  # 3 MB per chunk for reliable HTTP delivery

def upload():
    if not os.path.exists(APK_PATH):
        print(f"Error: {APK_PATH} does not exist!")
        return False

    total_size = os.path.getsize(APK_PATH)
    print(f"============================================================")
    print(f"Uploading Ankit EleveX v{VERSION} Final Release APK")
    print(f"File: {APK_PATH}")
    print(f"Total Size: {total_size:,} bytes ({total_size / (1024*1024):.2f} MB)")
    print(f"Target URL: {BASE_URL}")
    print(f"============================================================")

    # 1. Reset any pending temporary upload
    reset_url = f"{BASE_URL}?reset=1&version={VERSION}"
    try:
        with urllib.request.urlopen(reset_url, timeout=15) as resp:
            print("Reset state:", resp.read().decode('utf-8'))
    except Exception as e:
        print(f"Reset warning: {e}")

    # 2. Upload chunks
    with open(APK_PATH, "rb") as f:
        chunk_idx = 0
        bytes_sent = 0
        while True:
            chunk_data = f.read(CHUNK_SIZE)
            if not chunk_data:
                break

            bytes_sent += len(chunk_data)
            is_last = (bytes_sent >= total_size)

            url = f"{BASE_URL}?chunk_upload=1&chunk={chunk_idx}&last={'1' if is_last else '0'}&version={VERSION}"

            retries = 3
            success = False
            while retries > 0:
                try:
                    req = urllib.request.Request(
                        url,
                        data=chunk_data,
                        headers={
                            "Content-Type": "application/octet-stream",
                            "User-Agent": "EleveX-Chunk-Uploader/1.0"
                        }
                    )
                    with urllib.request.urlopen(req, timeout=60) as resp:
                        res_body = resp.read().decode('utf-8', errors='ignore')
                        percent = (bytes_sent * 100.0) / total_size
                        print(f"Chunk {chunk_idx:02d} [{len(chunk_data):,} B] ({percent:5.1f}%): {res_body.strip()}")
                        success = True
                        break
                except Exception as ex:
                    print(f"Retry {4 - retries} for chunk {chunk_idx}: {ex}")
                    retries -= 1
                    time.sleep(2)

            if not success:
                print(f"Failed to upload chunk {chunk_idx} after 3 attempts.")
                return False

            chunk_idx += 1

    print("\n------------------------------------------------------------")
    print("Verifying uploaded APK status on server...")
    status_url = f"{BASE_URL}?status=1&version={VERSION}"
    try:
        with urllib.request.urlopen(status_url, timeout=15) as sresp:
            status = json.loads(sresp.read().decode('utf-8'))
            print("Server Status:", json.dumps(status, indent=2))
            if status.get("exists") and status.get("size") == total_size:
                print("\n[SUCCESS] APK upload and byte integrity verified 100%!")
                return True
            else:
                print("\n[WARNING] Size mismatch or file not found on server.")
                return False
    except Exception as e:
        print("Status check error:", e)
        return False

if __name__ == "__main__":
    success = upload()
    sys.exit(0 if success else 1)
