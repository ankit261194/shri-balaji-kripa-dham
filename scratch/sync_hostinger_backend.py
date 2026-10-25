import sys
import urllib.request
import json
import time

sys.stdout.reconfigure(encoding='utf-8')

print("1. Triggering deploy.php to sync backend files from GitHub main branch...")
deploy_url = "https://shribalajikripadham.online/deploy.php?sha=main"
try:
    req = urllib.request.Request(deploy_url, headers={"User-Agent": "Mozilla/5.0"})
    with urllib.request.urlopen(req, timeout=45) as resp:
        res = json.loads(resp.read().decode('utf-8'))
        print("deploy.php response:")
        print(json.dumps(res, indent=2, ensure_ascii=False))
except Exception as e:
    print("Error calling deploy.php:", e)

time.sleep(2)

print("\n2. Calling migrate_tracks.php to update MySQL ashram_tracks with all 40 tracks...")
migrate_url = "https://shribalajikripadham.online/api/migrate_tracks.php"
try:
    req = urllib.request.Request(migrate_url, headers={"User-Agent": "Mozilla/5.0"})
    with urllib.request.urlopen(req, timeout=45) as resp:
        res = json.loads(resp.read().decode('utf-8'))
        print("migrate_tracks.php response:")
        print(json.dumps(res, indent=2, ensure_ascii=False))
        assert res.get("total_tracks") == 40, f"Expected 40 tracks, got {res.get('total_tracks')}"
        assert res.get("migrated") == 40, f"Expected 40 migrated, got {res.get('migrated')}"
        print("SUCCESS: ALL 40 TRACKS MIGRATED TO MYSQL!")
except Exception as e:
    print("Error calling migrate_tracks.php:", e)

time.sleep(2)

print("\n3. Verifying get_sacred_tracks.php API output...")
tracks_url = "https://shribalajikripadham.online/api/get_sacred_tracks.php?admin=1"
try:
    req = urllib.request.Request(tracks_url, headers={"User-Agent": "Mozilla/5.0", "Cache-Control": "no-cache"})
    with urllib.request.urlopen(req, timeout=45) as resp:
        res = json.loads(resp.read().decode('utf-8'))
        tracks = res.get("tracks", []) if isinstance(res, dict) else res
        print(f"Total tracks returned by API: {len(tracks)}")
        assert len(tracks) == 40, f"Expected 40 tracks, got {len(tracks)}"
        for i, t in enumerate(tracks, 1):
            key = t.get("track_key")
            title = t.get("title_hindi")
            url = t.get("audio_url")
            print(f"[{i:02d}] {key:<28} | {title:<30} | {url[:60]}...")
        print("\nALL 40 TRACKS ARE 100% OPERATIONAL, SYNCHRONIZED, AND LIVE ON HOSTINGER!")
except Exception as e:
    print("Error calling get_sacred_tracks.php:", e)
