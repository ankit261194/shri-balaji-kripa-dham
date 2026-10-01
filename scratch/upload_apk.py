import urllib.request
import os
import sys

apk_path = r"backend\downloads\ShriBalajiKripaDham-release.apk"
if not os.path.exists(apk_path):
    print("APK not found!")
    sys.exit(1)

total_size = os.path.getsize(apk_path)
chunk_size = 5 * 1024 * 1024 # 5 MB
num_chunks = (total_size + chunk_size - 1) // chunk_size

print(f"Total size: {total_size} bytes ({total_size / (1024*1024):.2f} MB), {num_chunks} chunks.")

with open(apk_path, "rb") as f:
    for i in range(num_chunks):
        chunk_data = f.read(chunk_size)
        is_last = 1 if (i == num_chunks - 1) else 0
        url = f"https://shribalajikripadham.online/sync_apk.php?chunk_upload=1&chunk={i}&last={is_last}"
        
        req = urllib.request.Request(
            url,
            data=chunk_data,
            headers={
                "Content-Type": "application/octet-stream",
                "User-Agent": "Balaji-Chunk-Uploader/1.0"
            },
            method="POST"
        )
        
        try:
            with urllib.request.urlopen(req, timeout=60) as resp:
                result = resp.read().decode("utf-8")
                print(f"Chunk {i+1}/{num_chunks}: {result}")
        except Exception as e:
            print(f"Error on chunk {i}: {e}")
            sys.exit(1)

print("All chunks uploaded successfully!")
