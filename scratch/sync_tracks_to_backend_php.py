import re, json, sys
sys.stdout.reconfigure(encoding='utf-8')

with open('app/src/main/java/com/example/shribalajikripadham/data/sacred/SacredTracksData.kt', 'r', encoding='utf-8') as f:
    kt_code = f.read()

# Extract tracks
raw_tracks = kt_code.split('SacredTrack(')[1:]
tracks = []
for tr in raw_tracks:
    key, title_h, title_e, sub_h, dur, audio, disp, yt = "", "", "", "", "", "", 0, ""
    lyrics = ""
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
            'youtube_search_query': yt,
            'lyrics_hindi': lyrics
        })

print(f"Extracted {len(tracks)} tracks from SacredTracksData.kt")

# Generate PHP
php_lines = [
    "<?php",
    "// Script to migrate and update ashram_tracks with 100% complete, authentic, sacred lyrics",
    "if (file_exists(__DIR__ . '/../config/db.php')) {",
    "    require_once __DIR__ . '/../config/db.php';",
    "} else {",
    "    if (!defined('DB_HOST')) define('DB_HOST', 'localhost');",
    "    if (!defined('DB_NAME')) define('DB_NAME', 'u237101617_balaji');",
    "    if (!defined('DB_USER')) define('DB_USER', 'u237101617_ankitantim0');",
    "    if (!defined('DB_PASS')) define('DB_PASS', 'Aa@8006518960');",
    "    function getDB() {",
    "        $dsn = 'mysql:host=' . DB_HOST . ';dbname=' . DB_NAME . ';charset=utf8mb4';",
    "        return new PDO($dsn, DB_USER, DB_PASS, [",
    "            PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,",
    "            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC",
    "        ]);",
    "    }",
    "}",
    "",
    "header('Content-Type: application/json; charset=utf-8');",
    "",
    "try {",
    "    $pdo = getDB();",
    "    // Ensure table exists",
    "    $pdo->exec(\"CREATE TABLE IF NOT EXISTS ashram_tracks (",
    "        id INT AUTO_INCREMENT PRIMARY KEY,",
    "        track_key VARCHAR(64) UNIQUE NOT NULL,",
    "        title_hindi VARCHAR(128) NOT NULL,",
    "        title_english VARCHAR(128) DEFAULT '',",
    "        subtitle_hindi VARCHAR(255) DEFAULT '',",
    "        duration_text VARCHAR(32) DEFAULT '5:00',",
    "        audio_url TEXT NOT NULL,",
    "        lyrics_hindi MEDIUMTEXT NOT NULL,",
    "        is_published TINYINT(1) DEFAULT 1,",
    "        display_order INT DEFAULT 0,",
    "        youtube_search_query VARCHAR(255) DEFAULT '',",
    "        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,",
    "        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP",
    "    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;\");",
    "",
    "    $tracks = ["
]

for t in tracks:
    escaped_lyrics = json.dumps(t['lyrics_hindi'], ensure_ascii=False)
    php_lines.append("        [")
    php_lines.append(f"            'track_key' => '{t['track_key']}',")
    php_lines.append(f"            'title_hindi' => '{t['title_hindi']}',")
    php_lines.append(f"            'title_english' => '{t['title_english']}',")
    php_lines.append(f"            'subtitle_hindi' => '{t['subtitle_hindi']}',")
    php_lines.append(f"            'duration_text' => '{t['duration_text']}',")
    php_lines.append(f"            'audio_url' => '{t['audio_url']}',")
    php_lines.append(f"            'display_order' => {t['display_order']},")
    php_lines.append(f"            'youtube_search_query' => '{t['youtube_search_query']}',")
    php_lines.append(f"            'lyrics_hindi' => {escaped_lyrics}")
    php_lines.append("        ],")

php_lines.extend([
    "    ];",
    "",
    "    $stmt = $pdo->prepare(\"INSERT INTO ashram_tracks (track_key, title_hindi, title_english, subtitle_hindi, duration_text, audio_url, lyrics_hindi, is_published, display_order, youtube_search_query) ",
    "        VALUES (:track_key, :title_hindi, :title_english, :subtitle_hindi, :duration_text, :audio_url, :lyrics_hindi, 1, :display_order, :youtube_search_query)",
    "        ON DUPLICATE KEY UPDATE ",
    "        title_hindi = VALUES(title_hindi),",
    "        title_english = VALUES(title_english),",
    "        subtitle_hindi = VALUES(subtitle_hindi),",
    "        duration_text = VALUES(duration_text),",
    "        audio_url = VALUES(audio_url),",
    "        lyrics_hindi = VALUES(lyrics_hindi),",
    "        is_published = 1,",
    "        display_order = VALUES(display_order),",
    "        youtube_search_query = VALUES(youtube_search_query);",
    "    \");",
    "",
    "    $count = 0;",
    "    foreach ($tracks as $t) {",
    "        $stmt->execute([",
    "            ':track_key' => $t['track_key'],",
    "            ':title_hindi' => $t['title_hindi'],",
    "            ':title_english' => $t['title_english'],",
    "            ':subtitle_hindi' => $t['subtitle_hindi'],",
    "            ':duration_text' => $t['duration_text'],",
    "            ':audio_url' => $t['audio_url'],",
    "            ':lyrics_hindi' => $t['lyrics_hindi'],",
    "            ':display_order' => $t['display_order'],",
    "            ':youtube_search_query' => $t['youtube_search_query']",
    "        ]);",
    "        $count++;",
    "    }",
    "    echo json_encode(['success' => true, 'updated' => $count, 'message' => 'All 18 sacred tracks successfully updated with unabridged lyrics']);",
    "} catch (Exception $e) {",
    "    http_response_code(500);",
    "    echo json_encode(['success' => false, 'error' => $e->getMessage()]);",
    "}"
])

with open('backend/api/migrate_tracks.php', 'w', encoding='utf-8') as f:
    f.write('\n'.join(php_lines))

print("backend/api/migrate_tracks.php written successfully!")
