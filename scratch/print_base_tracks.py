import sys
import re

sys.stdout.reconfigure(encoding='utf-8')

with open("app/src/main/java/com/example/shribalajikripadham/data/sacred/SacredTracksData.kt", "r", encoding="utf-8") as f:
    code = f.read()

blocks = [b for b in code.split('SacredTrack(') if 'id =' in b][:23]
for b in blocks:
    id_m = re.search(r'id\s*=\s*(\d+)', b).group(1)
    k = re.search(r'trackKey\s*=\s*"([^"]*)"', b).group(1)
    t = re.search(r'titleHindi\s*=\s*"([^"]*)"', b).group(1)
    a = re.search(r'audioUrl\s*=\s*"([^"]*)"', b).group(1)
    print(f"[{id_m:>2}] {k:<26} | {t:<30} | {a}")
