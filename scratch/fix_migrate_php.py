import json

with open(r'app\src\main\java\com\example\shribalajikripadham\data\sacred\SacredTracksData.kt', 'r', encoding='utf-8') as f:
    text = f.read()

raw_tracks = text.split('SacredTrack(')[1:]
tracks = []
for tr in raw_tracks:
    key, title_h, title_e, sub_h, dur, audio, disp, yt = '', '', '', '', '', '', 0, ''
    lyrics = ''
    for line in tr.split('\n'):
        line_s = line.strip()
        if line_s.startswith('trackKey = "'):
            key = line_s.split('"')[1]
        elif line_s.startswith('titleHindi = "'):
            title_h = line_s.split('"')[1]
        elif line_s.startswith('titleEnglish = "'):
            title_e = line_s.split('"')[1]
        elif line_s.startswith('subtitleHindi = "'):
            sub_h = line_s.split('"')[1]
        elif line_s.startswith('durationText = "'):
            dur = line_s.split('"')[1]
        elif line_s.startswith('audioUrl = "'):
            audio = line_s.split('"')[1]
        elif line_s.startswith('displayOrder = '):
            disp = int(line_s.split('=')[1].replace(',', '').strip())
        elif line_s.startswith('youtubeSearchQuery = "'):
            yt = line_s.split('"')[1]
    if 'lyricsHindi = """' in tr:
        part = tr.split('lyricsHindi = """')[1]
        lyrics = part.split('""".trimIndent()')[0]
    if key:
        tracks.append({
            'track_key': key,
            'title_hindi': title_h,
            'title_english': title_e,
            'subtitle_hindi': sub_h,
            'duration_text': dur,
            'audio_url': audio,
            'display_order': disp,
            'is_published': 1,
            'youtube_search_query': yt,
            'lyrics_hindi': lyrics
        })

tracks_json_str = json.dumps(tracks, ensure_ascii=False)
php_content = '<?php\n'
php_content += "header('Content-Type: application/json; charset=utf-8');\n"
php_content += "require_once __DIR__ . '/../config/db.php';\n\n"
php_content += "try {\n"
php_content += "    $pdo = getDB();\n"
php_content += "    if (!$pdo) throw new Exception('Database connection failed');\n\n"
php_content += "    $raw = " + json.dumps(tracks_json_str) + ";\n"
php_content += "    $defaultTracks = json_decode($raw, true);\n"
php_content += "    $count = 0;\n"
php_content += "    foreach ($defaultTracks as $t) {\n"
php_content += '        $st = $pdo->prepare("INSERT INTO sacred_tracks \n'
php_content += '            (track_key, title_hindi, title_english, subtitle_hindi, duration_text, audio_url, lyrics_hindi, is_published, display_order, youtube_search_query) \n'
php_content += '            VALUES (:tk, :th, :te, :sh, :dt, :au, :lh, :ip, :do, :yq)\n'
php_content += '            ON DUPLICATE KEY UPDATE \n'
php_content += '                title_hindi = VALUES(title_hindi),\n'
php_content += '                title_english = VALUES(title_english),\n'
php_content += '                subtitle_hindi = VALUES(subtitle_hindi),\n'
php_content += '                duration_text = VALUES(duration_text),\n'
php_content += '                audio_url = VALUES(audio_url),\n'
php_content += '                lyrics_hindi = VALUES(lyrics_hindi),\n'
php_content += '                is_published = VALUES(is_published),\n'
php_content += '                display_order = VALUES(display_order),\n'
php_content += '                youtube_search_query = VALUES(youtube_search_query)");\n\n'
php_content += '        $st->execute([\n'
php_content += "            ':tk' => $t['track_key'],\n"
php_content += "            ':th' => $t['title_hindi'],\n"
php_content += "            ':te' => $t['title_english'],\n"
php_content += "            ':sh' => $t['subtitle_hindi'],\n"
php_content += "            ':dt' => $t['duration_text'],\n"
php_content += "            ':au' => $t['audio_url'],\n"
php_content += "            ':lh' => $t['lyrics_hindi'],\n"
php_content += "            ':ip' => $t['is_published'],\n"
php_content += "            ':do' => $t['display_order'],\n"
php_content += "            ':yq' => $t['youtube_search_query']\n"
php_content += '        ]);\n'
php_content += '        $count++;\n'
php_content += '    }\n\n'
php_content += "    echo json_encode(['success' => true, 'migrated' => $count]);\n"
php_content += "} catch (Throwable $e) {\n"
php_content += "    echo json_encode(['success' => false, 'error' => $e->getMessage()]);\n"
php_content += "}\n"

with open(r'backend\api\migrate_tracks.php', 'w', encoding='utf-8') as f:
    f.write(php_content)

print(f"migrate_tracks.php successfully written with {len(tracks)} tracks!")
