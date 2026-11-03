import os
import sys
import time
import urllib.request
import json
import argparse
import hashlib

BASE_URL = "https://shribalajikripadham.online/sync_elevex_apk.php"
VERSION = "5.9.52"
CHUNK_SIZE = 3 * 1024 * 1024  # 3 MB per chunk

def calculate_sha256(filepath):
    h = hashlib.sha256()
    with open(filepath, "rb") as f:
        while chunk := f.read(65536):
            h.update(chunk)
    return h.hexdigest().lower()

def upload_apk(apk_path, filename):
    if not os.path.exists(apk_path):
        print(f"Error: {apk_path} does not exist!")
        return False, None, None

    total_size = os.path.getsize(apk_path)
    local_sha = calculate_sha256(apk_path)
    print("=" * 60)
    print(f"Uploading Standalone APK: {filename}")
    print(f"Source: {apk_path}")
    print(f"Size: {total_size:,} bytes ({total_size / (1024*1024):.2f} MB)")
    print(f"Local SHA-256: {local_sha}")
    print(f"Target URL: {BASE_URL}")
    print("=" * 60)

    # 1. Reset pending upload
    reset_url = f"{BASE_URL}?reset=1&filename={filename}&version={VERSION}"
    try:
        req = urllib.request.Request(reset_url, headers={"User-Agent": "EleveX-Uploader/2.0"})
        with urllib.request.urlopen(req, timeout=15) as resp:
            print("Reset response:", resp.read().decode('utf-8', errors='ignore'))
    except Exception as e:
        print(f"Reset warning: {e}")

    # 2. Upload chunks
    with open(apk_path, "rb") as f:
        chunk_idx = 0
        bytes_sent = 0
        while True:
            chunk_data = f.read(CHUNK_SIZE)
            if not chunk_data:
                break

            bytes_sent += len(chunk_data)
            is_last = (bytes_sent >= total_size)

            url = f"{BASE_URL}?chunk_upload=1&chunk={chunk_idx}&last={'1' if is_last else '0'}&filename={filename}&version={VERSION}"

            retries = 3
            success = False
            while retries > 0:
                try:
                    req = urllib.request.Request(
                        url,
                        data=chunk_data,
                        headers={
                            "Content-Type": "application/octet-stream",
                            "User-Agent": "EleveX-Chunk-Uploader/2.0"
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
                return False, None, None

            chunk_idx += 1

    print("\nVerifying server status...")
    status_url = f"{BASE_URL}?status=1&filename={filename}&version={VERSION}"
    try:
        req = urllib.request.Request(status_url, headers={"User-Agent": "EleveX-Uploader/2.0"})
        with urllib.request.urlopen(req, timeout=15) as sresp:
            status = json.loads(sresp.read().decode('utf-8'))
            print("Server Status:", json.dumps(status, indent=2))
            if status.get("exists") and status.get("size") == total_size:
                print(f"\n[SUCCESS] {filename} uploaded and verified ({total_size} bytes)!")
                return True, total_size, local_sha
            else:
                print("\n[WARNING] Size mismatch or file not found on server.")
                return False, total_size, local_sha
    except Exception as e:
        print("Status check error:", e)
        return False, total_size, local_sha

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--apk", required=True, help="Path to APK file")
    parser.add_argument("--filename", required=True, help="Remote filename")
    args = parser.parse_args()

    ok, size, sha = upload_apk(args.apk, args.filename)
    if ok:
        print(f"RESULT: SUCCESS | Size: {size} | SHA: {sha}")
        sys.exit(0)
    else:
        print("RESULT: FAILED")
        sys.exit(1)
