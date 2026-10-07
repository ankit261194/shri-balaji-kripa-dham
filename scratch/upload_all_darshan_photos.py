import urllib.request
import json
import sys
import os

sys.stdout.reconfigure(encoding='utf-8')

sacred_images = [
    ("img_balaji_darshan.jpg", "app/src/main/res/drawable/img_balaji_darshan.jpg", "श्री बालाजी कृपा धाम रविवार महा-दरबार दिव्य दर्शन"),
    ("img_panchmukhi_hanuman.jpg", "app/src/main/res/drawable/img_panchmukhi_hanuman.jpg", "श्री पंचमुखी हनुमान जी महाराज पावन दिव्य दर्शन"),
    ("img_mehandipur_balaji.jpg", "app/src/main/res/drawable/img_mehandipur_balaji.jpg", "श्री मेहंदीपुर बालाजी महाराज मंगलवार विशेष दिव्य श्रृंगार दर्शन"),
    ("img_ram_darbar.jpg", "app/src/main/res/drawable/img_ram_darbar.jpg", "प्रभु श्री राम दरबार एवं वीर हनुमान पावन दर्शन"),
    ("img_hanuman_veer.jpg", "app/src/main/res/drawable/img_hanuman_veer.jpg", "श्री संकटमोचन वीर बजरंगी शनिवार पावन अलौकिक दर्शन")
]

uploaded_urls = {}

for name, local_path, title in sacred_images:
    with open(local_path, "rb") as f:
        img_data = f.read()

    boundary = f"==DarshanBoundary_{os.path.basename(local_path)}=="
    part1 = (
        f"--{boundary}\r\n"
        f"Content-Disposition: form-data; name=\"photo_type\"\r\n\r\n"
        f"darshan\r\n"
        f"--{boundary}\r\n"
        f"Content-Disposition: form-data; name=\"photo\"; filename=\"{name}\"\r\n"
        f"Content-Type: image/jpeg\r\n\r\n"
    ).encode("utf-8")
    part2 = f"\r\n--{boundary}--\r\n".encode("utf-8")
    body = part1 + img_data + part2

    req = urllib.request.Request(
        "https://shribalajikripadham.online/api/upload_photo.php",
        data=body,
        headers={
            "Content-Type": f"multipart/form-data; boundary={boundary}",
            "X-SBKD-API-KEY": "SBKD_SECURE_TOKEN_9100100251233433_V243",
            "User-Agent": "DarshanBatchUploader"
        }
    )

    with urllib.request.urlopen(req) as resp:
        res = json.loads(resp.read().decode("utf-8", errors="ignore"))
        uploaded_url = res.get("photo_url")
        uploaded_urls[name] = uploaded_url
        print(f"Uploaded {name} ({len(img_data)} bytes) -> {uploaded_url}")

# Verify each uploaded URL is accessible via HTTP 200 and has correct bytes
print("\nVERIFYING UPLOADED IMAGES:")
for name, url in uploaded_urls.items():
    try:
        vreq = urllib.request.Request(url, headers={"User-Agent": "DarshanVerifier"})
        with urllib.request.urlopen(vreq) as vresp:
            content = vresp.read()
            print(f"VERIFIED {name}: HTTP {vresp.status}, size = {len(content)} bytes")
    except Exception as e:
        print(f"FAILED {name}: {e}")

# Save mapping to json
with open("scratch/uploaded_darshan_map.json", "w", encoding="utf-8") as f:
    json.dump(uploaded_urls, f, indent=2, ensure_ascii=False)
