import urllib.request, json, sys
sys.stdout.reconfigure(encoding='utf-8')

url = 'https://shribalajikripadham.online/api/daily_darshan.php'
req = urllib.request.Request(url, headers={'User-Agent': 'TestInspector'})
with urllib.request.urlopen(req) as resp:
    data = json.loads(resp.read().decode('utf-8'))
    print("DAILY DARSHAN RESPONSE:")
    print(json.dumps(data, indent=2, ensure_ascii=False))

# Check media files
media_files = [
    "balaji_darshan_today.jpg",
    "img_mehandipur_balaji.jpg",
    "img_hanuman_veer.jpg",
    "img_balaji_darshan.jpg",
    "img_panchmukhi_hanuman.jpg",
    "img_ram_darbar.jpg"
]

print("\nCHECKING MEDIA FILES STATUS:")
for mf in media_files:
    murl = f"https://shribalajikripadham.online/media/{mf}"
    try:
        mreq = urllib.request.Request(murl, headers={'User-Agent': 'TestInspector'})
        with urllib.request.urlopen(mreq) as mresp:
            print(f"{mf}: HTTP {mresp.status}, length: {len(mresp.read())} bytes")
    except urllib.error.HTTPError as e:
        print(f"{mf}: HTTP {e.code} ({e.reason})")
    except Exception as e:
        print(f"{mf}: Error {e}")
