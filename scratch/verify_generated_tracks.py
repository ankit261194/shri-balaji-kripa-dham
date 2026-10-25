import sys
import re

sys.stdout.reconfigure(encoding='utf-8')

with open("app/src/main/java/com/example/shribalajikripadham/data/sacred/SacredTracksData.kt", "r", encoding="utf-8") as f:
    code = f.read()

raw_blocks = code.split("SacredTrack(")
blocks = [b for b in raw_blocks if "id =" in b]
print(f"Total track instances in SACRED_TRACKS: {len(blocks)}")

for i, b in enumerate(blocks, 1):
    def get_val(key):
        m = re.search(rf'{key}\s*=\s*"""(.*?)"""', b, re.DOTALL)
        if m: return m.group(1).strip()
        m = re.search(rf'{key}\s*=\s*"([^"]*)"', b)
        if m: return m.group(1).strip()
        return ""
    
    id_m = re.search(r'id\s*=\s*(\d+)', b)
    t_id = id_m.group(1) if id_m else str(i)
    t_key = get_val("trackKey")
    t_hindi = get_val("titleHindi")
    t_dur = get_val("durationText")
    audio = get_val("audioUrl")
    lyrics = get_val("lyricsHindi")
    non_empty = [line.strip() for line in lyrics.split('\n') if line.strip()]
    first_two = " // ".join(non_empty[:2]) if non_empty else "(EMPTY)"
    
    print(f"[{t_id:>2}] {t_key:<28} | {t_hindi:<32} | {t_dur:<6} | lyrics_len: {len(lyrics):>5}")
    print(f"     audio: {audio}")
    print(f"     text:  {first_two[:75]}")
