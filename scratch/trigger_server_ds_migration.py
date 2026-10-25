import sys
import urllib.request
import json
import time

sys.stdout.reconfigure(encoding='utf-8')

ds_keys = [
    "durga_saptashati_kavach",
    "durga_saptashati_argala",
    "durga_saptashati_keelak",
    "durga_saptashati_ch1",
    "durga_saptashati_ch2",
    "durga_saptashati_ch3",
    "durga_saptashati_ch4",
    "durga_saptashati_ch5",
    "durga_saptashati_ch6",
    "durga_saptashati_ch7",
    "durga_saptashati_ch8",
    "durga_saptashati_ch9",
    "durga_saptashati_ch10",
    "durga_saptashati_ch11",
    "durga_saptashati_ch12",
    "durga_saptashati_ch13",
    "durga_saptashati_kshama"
]

print(f"Starting server-side CDN migration for all {len(ds_keys)} Durga Saptashati chapters...")

new_cdn_urls = {}

for i, key in enumerate(ds_keys, 1):
    url = f"https://shribalajikripadham.online/api/fetch_saptashati_audio.php?key={key}"
    print(f"[{i:02d}/{len(ds_keys):02d}] Migrating {key} on Hostinger server...", end=" ", flush=True)
    t0 = time.time()
    try:
        req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
        with urllib.request.urlopen(req, timeout=180) as resp:
            data = json.loads(resp.read().decode('utf-8'))
            res = data.get("results", {}).get(key, {})
            status = res.get("status")
            size_mb = res.get("size_mb", "0 MB")
            cdn_url = res.get("cdn_url", "")
            elapsed = time.time() - t0
            print(f"DONE ({status}, {size_mb}, {elapsed:.1f}s)")
            print(f"       CDN: {cdn_url}")
            new_cdn_urls[key] = cdn_url
    except Exception as e:
        print(f"FAILED: {e}")

print("\n--- Summary of All Migrated Durga Saptashati Hostinger CDN URLs ---")
print(json.dumps(new_cdn_urls, indent=2, ensure_ascii=False))

with open("scratch/saptashati_cdn_urls.json", "w", encoding="utf-8") as f:
    json.dump(new_cdn_urls, f, indent=2, ensure_ascii=False)
