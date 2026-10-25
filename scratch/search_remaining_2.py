import subprocess
import json

def search(q):
    cmd = ['yt-dlp', '--default-search', 'ytsearch5:', '--dump-json', '--no-playlist', q]
    res = subprocess.run(cmd, capture_output=True, text=True, encoding='utf-8', errors='ignore')
    for line in res.stdout.strip().split('\n'):
        if line:
            try:
                d = json.loads(line)
                print(f"[{d.get('id')}]: {d.get('title')} ({d.get('duration')}s) - {d.get('uploader')}")
            except: pass

print('--- Search 2: Meri Naav Chali ---')
search('Bajrang Bali Meri Naav Chali')
search('Meri Naav Chali Ram Avtar Sharma')
print('--- Search 3: Shabar Mantra ---')
search('Hanuman Shabar Mantra')
search('Om Guruji Ko Aadesh Om Namo Vajra Ka Kotha')
