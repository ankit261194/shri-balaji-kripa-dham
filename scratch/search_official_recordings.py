import subprocess
import json

def search(label, query):
    print(f"=== {label} ===")
    cmd = ['yt-dlp', '--default-search', 'ytsearch3:', '--dump-json', '--no-playlist', query]
    res = subprocess.run(cmd, capture_output=True, text=True, encoding='utf-8', errors='ignore')
    for line in res.stdout.strip().split('\n'):
        if line:
            try:
                data = json.loads(line)
                print(f"[{data.get('id')}]: {data.get('title')} ({data.get('duration')}s) - {data.get('uploader')}")
            except Exception as e:
                pass

if __name__ == '__main__':
    search('1. Aaj Mangalwar Hai', 'aaj mangalwar hai mahaveer ka vaar hai bhajan')
    search('2. Meri Naav Chali Bajrang Bali', 'meri naav chali bajrang bali bhajan')
    search('3. Shabar Veer Hanumant Mantra', 'veer hanumant shabar mantra hanuman')
    search('4. Jai Laxmi Ramana', 'om jai lakshmi ramana satyanarayan aarti anuradha paudwal')
