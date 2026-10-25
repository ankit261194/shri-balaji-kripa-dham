import sys
import re
sys.stdout.reconfigure(encoding='utf-8')

with open('app/src/main/java/com/example/shribalajikripadham/data/sacred/SacredTracksData.kt', 'r', encoding='utf-8') as f:
    content = f.read()

tracks = re.findall(r'SacredTrack\((.*?)\n\s*\)', content, re.DOTALL)
print(f"Total SacredTrack entries: {len(tracks)}")
for i, t in enumerate(tracks, 1):
    def get_field(k):
        m = re.search(rf'{k}\s*=\s*"([^"]*)"', t)
        if m: return m.group(1)
        m_num = re.search(rf'{k}\s*=\s*(\d+)', t)
        if m_num: return m_num.group(1)
        return ""
    
    t_id = get_field("id")
    key = get_field("trackKey")
    title = get_field("titleHindi")
    audio = get_field("audioUrl")
    print(f"[{i}] id={t_id} key={key} title={title}")
    print(f"    audio: {audio[:60]}...")
