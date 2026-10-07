import urllib.request
import json

def check(url):
    req = urllib.request.Request(url, headers={'User-Agent': 'Balaji-Checker/1.0', 'Cache-Control': 'no-cache'})
    with urllib.request.urlopen(req, timeout=15) as r:
        return r.read()

v_data = json.loads(check('https://shribalajikripadham.online/version.json').decode('utf-8'))
print('[1] VERSION CHECK:')
print('  version_code:', v_data.get('version_code'))
print('  version_name:', v_data.get('version_name'))
print('  apk_url:', v_data.get('apk_url'))

req_apk = urllib.request.Request('https://shribalajikripadham.online/downloads/ShriBalajiKripaDham-release.apk', method='HEAD')
with urllib.request.urlopen(req_apk, timeout=15) as r:
    print('[2] RELEASE APK SIZE:', r.headers.get('Content-Length'), 'bytes')

req_apk149 = urllib.request.Request('https://shribalajikripadham.online/downloads/ShriBalajiKripaDham-v149.apk', method='HEAD')
with urllib.request.urlopen(req_apk149, timeout=15) as r:
    print('[3] V149 APK SIZE:', r.headers.get('Content-Length'), 'bytes')

tracks_raw = check('https://shribalajikripadham.online/api/get_sacred_tracks.php')
tracks = json.loads(tracks_raw.decode('utf-8'))
tracks_list = tracks.get('tracks', [])
print('[4] SACRED TRACKS COUNT:', len(tracks_list))
for t in tracks_list:
    tid = t.get('id')
    lyrics_len = len(t.get('lyrics', ''))
    print(f'   - Track {tid}: {lyrics_len} chars')

config_raw = check('https://shribalajikripadham.online/api/live_config.php')
config = json.loads(config_raw.decode('utf-8'))
settings = config.get('ashram_settings', {})
print('[5] LIVE ASHRAM SETTINGS:')
print('  darbar_enabled:', settings.get('darbar_enabled'))
print('  havan_estimated_cost:', settings.get('havan_estimated_cost'))
print('  havan_rules_notice length:', len(settings.get('havan_rules_notice', '')))
