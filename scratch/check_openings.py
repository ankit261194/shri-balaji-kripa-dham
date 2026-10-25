import sys
import re
sys.stdout.reconfigure(encoding='utf-8')

with open('app/src/main/java/com/example/shribalajikripadham/data/sacred/SacredTracksData.kt', 'r', encoding='utf-8') as f:
    orig_code = f.read()

track_blocks = re.findall(r'SacredTrack\((.*?)\n\s*\)', orig_code, re.DOTALL)

for i, b in enumerate(track_blocks, 1):
    def get_field(name):
        m = re.search(rf'{name}\s*=\s*"([^"]*)"', b)
        if m: return m.group(1)
        m_str = re.search(rf'{name}\s*=\s*"""(.*?)"""', b, re.DOTALL)
        if m_str: return m_str.group(1).strip()
        return ""
    
    t_key = get_field("trackKey")
    if not t_key: continue
    title = get_field("titleHindi")
    lyrics = get_field("lyricsHindi")
    non_empty = [line.strip() for line in lyrics.split('\n') if line.strip()]
    first_lines = " // ".join(non_empty[:2])
    print(f"[{t_key}] -> Title: {title}")
    print(f"       Opening: {first_lines[:80]}")
