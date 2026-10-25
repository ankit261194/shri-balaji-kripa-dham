import sys
import re
sys.stdout.reconfigure(encoding='utf-8')

with open('app/src/main/java/com/example/shribalajikripadham/data/sacred/SacredTracksData.kt', 'r', encoding='utf-8') as f:
    orig_code = f.read()

track_blocks = re.findall(r'SacredTrack\((.*?)\n\s*\)', orig_code, re.DOTALL)
print(f"Total blocks in SacredTracksData: {len(track_blocks)}")

for i, b in enumerate(track_blocks, 1):
    def get_field(name):
        m = re.search(rf'{name}\s*=\s*"([^"]*)"', b)
        if m: return m.group(1)
        m_str = re.search(rf'{name}\s*=\s*"""(.*?)"""', b, re.DOTALL)
        if m_str: return m_str.group(1).strip()
        return ""
    
    t_key = get_field("trackKey")
    title = get_field("titleHindi")
    lyrics = get_field("lyricsHindi")
    audio = get_field("audioUrl")
    first_line_lyrics = lyrics.split('\n')[0] if lyrics else ""
    print(f"[{i}] key: {t_key:28} | title: {title:30} | first_line: {first_line_lyrics[:35]}")
