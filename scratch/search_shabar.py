import subprocess
import json

cmd = ['yt-dlp', '--dump-json', '--no-playlist', 'ytsearch5:hanuman shabar mantra']
res = subprocess.run(cmd, capture_output=True, text=True, encoding='utf-8', errors='ignore')
for line in res.stdout.strip().split('\n'):
    if line:
        try:
            d = json.loads(line)
            print(f"{d.get('id')}: {d.get('title')} ({d.get('duration')}s) - {d.get('uploader')}")
        except: pass
