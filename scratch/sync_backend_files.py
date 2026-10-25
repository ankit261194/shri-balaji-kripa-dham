import urllib.request
import json
import sys

sys.stdout.reconfigure(encoding='utf-8')

url = "https://shribalajikripadham.online/deploy.php?file=api/migrate_tracks.php,version.json"
req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})

try:
    with urllib.request.urlopen(req, timeout=30) as resp:
        res = json.loads(resp.read().decode('utf-8'))
        print("Deploy sync response:")
        print(json.dumps(res, indent=2, ensure_ascii=False))
except Exception as e:
    print(f"Error calling deploy.php: {e}")
    sys.exit(1)

# Now call migrate_tracks.php to apply database updates
print("\nCalling migrate_tracks.php to update MySQL...")
mig_url = "https://shribalajikripadham.online/api/migrate_tracks.php"
req_m = urllib.request.Request(mig_url, headers={"User-Agent": "Mozilla/5.0"})

try:
    with urllib.request.urlopen(req_m, timeout=60) as resp:
        m_res = json.loads(resp.read().decode('utf-8'))
        print("Migrate tracks response:")
        print(json.dumps(m_res, indent=2, ensure_ascii=False))
except Exception as e:
    print(f"Error calling migrate_tracks.php: {e}")
    sys.exit(1)

# Verify get_sacred_tracks.php
print("\nVerifying live get_sacred_tracks.php...")
tracks_url = "https://shribalajikripadham.online/api/get_sacred_tracks.php"
req_t = urllib.request.Request(tracks_url, headers={"User-Agent": "Mozilla/5.0"})

try:
    with urllib.request.urlopen(req_t, timeout=30) as resp:
        t_res = json.loads(resp.read().decode('utf-8'))
        tracks = t_res.get("tracks", [])
        print(f"Total live sacred tracks in MySQL: {len(tracks)}")
        assert len(tracks) == 40, f"Expected 40 tracks, found {len(tracks)}"
        # Verify first and last
        print("Track 1:", tracks[0].get("title_hindi"), "URL:", tracks[0].get("audio_url"))
        print("Track 40:", tracks[39].get("title_hindi"), "URL:", tracks[39].get("audio_url"))
        print("\nALL 40 SACRED TRACKS 100% VERIFIED LIVE IN MYSQL!")
except Exception as e:
    print(f"Error verifying get_sacred_tracks.php: {e}")
    sys.exit(1)
