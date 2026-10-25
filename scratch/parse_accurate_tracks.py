import sys
import re
sys.stdout.reconfigure(encoding='utf-8')

with open('app/src/main/java/com/example/shribalajikripadham/data/sacred/SacredTracksData.kt', 'r', encoding='utf-8') as f:
    orig_code = f.read()

# Split by "SacredTrack("
blocks = orig_code.split("SacredTrack(")[1:]
print(f"Total raw blocks: {len(blocks)}")

parsed_tracks = []
for i, block in enumerate(blocks, 1):
    def get_val(key):
        # Check string """..."""
        m = re.search(rf'{key}\s*=\s*"""(.*?)"""', block, re.DOTALL)
        if m: return m.group(1).strip()
        # Check string "..."
        m = re.search(rf'{key}\s*=\s*"([^"]*)"', block)
        if m: return m.group(1).strip()
        return ""
    
    t_key = get_val("trackKey")
    if not t_key: continue
    t_hindi = get_val("titleHindi")
    t_eng = get_val("titleEnglish")
    sub_hindi = get_val("subtitleHindi")
    dur = get_val("durationText")
    audio = get_val("audioUrl")
    lyrics = get_val("lyricsHindi")
    yt = get_val("youtubeSearchQuery")
    
    non_empty = [line.strip() for line in lyrics.split('\n') if line.strip()]
    opening = non_empty[0] if non_empty else "(NO LYRICS)"
    if len(non_empty) > 1:
        opening += " | " + non_empty[1]
    
    print(f"[{i}] key: {t_key:28} | title: {t_hindi:25}")
    print(f"     opening: {opening[:60]}")
    print(f"     audio:   {audio}")
    
    parsed_tracks.append({
        "trackKey": t_key,
        "titleHindi": t_hindi,
        "titleEnglish": t_eng,
        "subtitleHindi": sub_hindi,
        "durationText": dur,
        "audioUrl": audio,
        "lyricsHindi": lyrics,
        "youtubeSearchQuery": yt
    })

print(f"\nSuccessfully parsed {len(parsed_tracks)} tracks!")
