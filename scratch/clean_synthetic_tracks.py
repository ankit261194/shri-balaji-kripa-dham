import re

file_path = 'app/src/main/java/com/example/shribalajikripadham/data/sacred/SacredTracksData.kt'

with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# Synthetic keys to eliminate
BANNED_KEYS = {
    'pretraj_chalisa',
    'khatu_shyam_aarti',
    'satyanarayan_aarti',
    'hanuman_veer_raksha_mantra',
    'bhajan_meri_naav_chali',
    'bhajan_aaj_mangalwar_hai'
}

# Find beginning of listOf( and end of listOf(...)
start_marker = 'val SACRED_TRACKS: List<SacredTrack> = listOf('
start_idx = content.find(start_marker)
if start_idx == -1:
    print("Could not find start marker!")
    exit(1)

body_start = start_idx + len(start_marker)
# Find the matching closing ')' for listOf(
# In this file, listOf is at the top level of the object
end_idx = content.rfind(')')
# Let's find the 'listOf(' closing paren
# It ends right before the closing brace of object SacredTracksData
obj_end = content.rfind('}')
body_end = content.rfind(')', 0, obj_end)

header = content[:body_start]
footer = content[body_end:]
body = content[body_start:body_end]

# Split tracks by SacredTrack(
parts = body.split('SacredTrack(')
header_prefix = parts[0] # usually whitespace/newlines
track_chunks = parts[1:]

cleaned_tracks = []
for chunk in track_chunks:
    # check key
    m = re.search(r'trackKey\s*=\s*"([^"]+)"', chunk)
    if not m:
        continue
    key = m.group(1)
    if key in BANNED_KEYS:
        print(f"Purging synthetic track: {key}")
        continue
    cleaned_tracks.append((key, chunk))

print(f"Total authentic tracks kept: {len(cleaned_tracks)}")

# Now rebuild chunks with renumbered id and displayOrder
rebuilt_chunks = []
for i, (key, chunk) in enumerate(cleaned_tracks, 1):
    # remove trailing commas/whitespace for clean formatting
    c = chunk.strip()
    if c.endswith(','):
        c = c[:-1].strip()
    
    # renumber id = \d+
    c = re.sub(r'id\s*=\s*\d+', f'id = {i}', c, count=1)
    # renumber displayOrder = \d+
    c = re.sub(r'displayOrder\s*=\s*\d+', f'displayOrder = {i}', c, count=1)
    
    rebuilt_chunks.append(f"    SacredTrack({c}")

new_body = "\n" + ",\n".join(rebuilt_chunks) + "\n"

new_content = header + new_body + footer

# Update comment at top
new_content = new_content.replace(
    '40 Authentic pre-loaded devotional tracks',
    '34 100% Authentic, Pure Traditional Devotional Tracks (Zero Synthetic/TTS)'
)

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(new_content)

print("Successfully written cleaned authentic tracks to SacredTracksData.kt!")
