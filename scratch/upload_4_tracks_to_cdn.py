import urllib.request
import json
import os
import ssl

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

files_to_upload = [
    ("aaj_mangalwar.m4a", "aaj_mangalwar.m4a", "audio/mp4"),
    ("meri_naav_chali.m4a", "meri_naav_chali.m4a", "audio/mp4"),
    ("hanuman_shabar_mantra.m4a", "hanuman_shabar_mantra.m4a", "audio/mp4"),
    ("satyanarayan_aarti_laxmi_ramana.m4a", "satyanarayan_aarti_laxmi_ramana.m4a", "audio/mp4")
]

results = {}

for local_name, remote_name, mime in files_to_upload:
    local_path = os.path.join("scratch", local_name)
    if not os.path.exists(local_path):
        print(f"ERROR: {local_path} does not exist!")
        continue
    
    size = os.path.getsize(local_path)
    print(f"Uploading {local_name} ({size/(1024*1024):.2f} MB) to Hostinger CDN...")
    
    boundary = '----WebKitFormBoundarySbkdAudio' + os.urandom(8).hex()
    with open(local_path, 'rb') as f:
        file_bytes = f.read()

    body = bytearray()
    body.extend(f'--{boundary}\r\n'.encode('utf-8'))
    body.extend(f'Content-Disposition: form-data; name="audio"; filename="{remote_name}"\r\n'.encode('utf-8'))
    body.extend(f'Content-Type: {mime}\r\n\r\n'.encode('utf-8'))
    body.extend(file_bytes)
    body.extend(f'\r\n--{boundary}--\r\n'.encode('utf-8'))

    req = urllib.request.Request(
        'https://shribalajikripadham.online/api/upload_audio.php',
        data=bytes(body),
        headers={
            'Content-Type': f'multipart/form-data; boundary={boundary}',
            'X-SBKD-API-KEY': 'SBKD_SECURE_TOKEN_9100100251233433_V243'
        },
        method='POST'
    )

    try:
        with urllib.request.urlopen(req, context=ctx, timeout=180) as resp:
            res = json.loads(resp.read().decode('utf-8'))
            print(f"SUCCESS: {local_name} -> {res.get('audio_url')}")
            results[local_name] = res.get('audio_url')
    except Exception as e:
        print(f"FAILED {local_name}: {e}")

with open("scratch/uploaded_4_tracks.json", "w", encoding="utf-8") as f:
    json.dump(results, f, indent=2)

print("\nAll done! Results saved to scratch/uploaded_4_tracks.json")
