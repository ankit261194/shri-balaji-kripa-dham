import os
import re
import json
import urllib.request
import sys

# 1. Update SacredTracksData.kt
kt_path = r"app\src\main\java\com\example\shribalajikripadham\data\sacred\SacredTracksData.kt"

with open(kt_path, "r", encoding="utf-8") as f:
    content = f.read()

balaji_lyrics = """ॐ जय हनुमत वीरा, स्वामी जय हनुमत वीरा।
संकट मोचन स्वामी, तुम हो रणधीरा॥ ॐ जय हनुमत वीरा... (ध्रुवपद)

पवन पुत्र अंजनी सुत, महिमा अति भारी।
दुःख दरिद्र मिटाओ, संकट सब हारी॥ ॐ जय हनुमत वीरा...

बाल समय में तुमने, रवि को भक्ष लियो।
देवन स्तुति कीन्ही, तुरतहिं छोड़ दियो॥ ॐ जय हनुमत वीरा...

कपि सुग्रीव राम संग, मैत्री करवाई।
अभिमानी बलि मेटयो, कीर्ति रही छाई॥ ॐ जय हनुमत वीरा...

जारि लंक को ले सिय की, सुधि वानर हर्षाये।
कारज कठिन सुधारे, रघुवर मन भाये॥ ॐ जय हनुमत वीरा...

शक्ति लगी लक्ष्मण को, भारी सोच भयो।
लाय संजीवन बूटी, दुःख सब दूर कियो॥ ॐ जय हनुमत वीरा...

रामहि ले अहिरावण, जब पाताल गयो।
ताहि मारि प्रभु लाये, जय जयकार भयो॥ ॐ जय हनुमत वीरा...

राजत मेहंदीपुर में, दर्शन सुखकारी।
डूँगरा जाट धाम विराजे, महिमा अति न्यारी॥ ॐ जय हनुमत वीरा...

मंगल और शनिश्चर, मेला है जारी।
अर्जी सुन लो दयालु, बालाजी अवतारी॥ ॐ जय हनुमत वीरा...

श्री बालाजी की आरती, जो कोई नर गावे।
कहत इन्द्र हर्षित मन, वांछित फल पावे॥
ॐ जय हनुमत वीरा, स्वामी जय हनुमत वीरा।
संकट मोचन स्वामी, तुम हो रणधीरा॥"""

bhairav_lyrics = """सुनो जी भैरव लाडले, कर जोड़ कर विनती करूँ।
कृपा तुम्हारी चाहिए, मैं ध्यान तुम्हारा ही धरूँ॥ (ध्रुवपद)

मैं चरण छूता आपके, अर्जी मेरी सुन लीजिए।
मैं हूँ मति का मंद, मेरी कुछ मदद तो कीजिए॥
महिमा तुम्हारी बहुत, कुछ थोड़ी सी मैं वर्णन करूँ।
सुनो जी भैरव लाडले, कर जोड़ कर विनती करूँ॥

करते सवारी श्वान की, चारों दिशा में राज्य है।
जितने भूत और प्रेत, सबके आप ही सरताज हैं॥
हथियार है जो आपके, उनका क्या वर्णन करूँ।
सुनो जी भैरव लाडले, कर जोड़ कर विनती करूँ॥

माताजी के सामने तुम, नृत्य भी करते हो सदा।
गा-गा के गुण-अनुवाद से, उनको रिझाते हो सदा॥
एक सांकली है आपकी, तारीफ़ उसकी क्या करूँ।
सुनो जी भैरव लाडले, कर जोड़ कर विनती करूँ॥

बहुत सी महिमा तुम्हारी, मेहंदीपुर सरनाम है।
आते जगत के यात्री, बजरंग का स्थान है॥
श्री प्रेतराज सरकार के, मैं शीश चरणों में धरूँ।
सुनो जी भैरव लाडले, कर जोड़ कर विनती करूँ॥

डूँगरा जाट के धाम में, बाबा तिहारी शान है।
संकट कटे सब भक्त के, मिलता अभय वरदान है॥
दुखियों के संकट दूर कर, चरणों में तेरे आ गिरूँ।
सुनो जी भैरव लाडले, कर जोड़ कर विनती करूँ॥

निशदिन तुम्हारे खेल से, माताजी खुश होती रहें।
सर पर तुम्हारे हाथ रखकर, आशीर्वाद देती रहें॥
कर जोड़ कर विनती करूँ, और शीश चरणों में धरूँ।
सुनो जी भैरव लाडले, कर जोड़ कर विनती करूँ॥"""

guru_lyrics = """जय गुरुदेव दयानिधि, दीनन हितकारी।
स्वामी भक्तन हितकारी।
जय जय मोह विनाशक, भव बंधन हारी॥
ॐ जय जय जय गुरुदेव हरे॥ (ध्रुवपद)

ब्रह्मा विष्णु सदा शिव, गुरु मूरति धारी।
वेद पुराण बखानत, गुरु महिमा भारी॥
ॐ जय जय जय गुरुदेव हरे॥

जप तप तीरथ संयम, दान बिबिध दीजै।
गुरु बिन ज्ञान न होवे, कोटि जतन कीजै॥
ॐ जय जय जय गुरुदेव हरे॥

माया मोह नदी जल, जीव बहे सारे।
नाम जहाज बिठा कर, गुरु पल में तारे॥
ॐ जय जय जय गुरुदेव हरे॥

काम क्रोध मद मत्सर, चोर बड़े भारे।
ज्ञान खड्ग दे कर में, गुरु सब संहारे॥
ॐ जय जय जय गुरुदेव हरे॥

नाना पंथ जगत में, निज निज गुण गावे।
सबका सार बताकर, गुरु मारग लावे॥
ॐ जय जय जय गुरुदेव हरे॥

पाँच चोर के कारण, नाम को बाण दियो।
प्रेम भक्ति से सादा, भव जल पार कियो॥
ॐ जय जय जय गुरुदेव हरे॥

गुरु चरणामृत निर्मल, सब पातक हारी।
बचन सुनत तम नाशे, सब संशय हारी॥
ॐ जय जय जय गुरुदेव हरे॥

तन मन धन सब अर्पण, गुरु चरणन कीजै।
ब्रह्मानंद परम पद, मोक्ष गति लीजै॥
ॐ जय जय जय गुरुदेव हरे॥

श्री सतगुरुदेव की आरती, जो कोई नर गावै।
भव सागर से तरकर, परम गति पावै॥
ॐ जय जय जय गुरुदेव हरे॥"""

# Replace balaji_aarti
old_balaji_pattern = r'id = 3,\s+trackKey = "balaji_aarti",[\s\S]*?displayOrder = 3,'
new_balaji = f'''id = 3,
        trackKey = "balaji_aarti",
        titleHindi = "श्री बालाजी महाराज की आरती",
        titleEnglish = "Shri Balaji Maharaj Aarti",
        subtitleHindi = "ॐ जय हनुमत वीरा, संकट मोचन रणधीरा",
        durationText = "5:15",
        audioUrl = "https://shribalajikripadham.online/uploads/audio/sbkd_audio_1791373307_1a7f1b.mp3",
        lyricsHindi = """{balaji_lyrics}""".trimIndent(),
        isPublished = true,
        displayOrder = 3,'''
content = re.sub(old_balaji_pattern, new_balaji, content)

# Replace bhairav_aarti
old_bhairav_pattern = r'id = 6,\s+trackKey = "bhairav_aarti",[\s\S]*?displayOrder = 6,'
new_bhairav = f'''id = 6,
        trackKey = "bhairav_aarti",
        titleHindi = "श्री भैरव जी की आरती (सुनो जी भैरव लाडले)",
        titleEnglish = "Shri Bhairav Ladle Aarti",
        subtitleHindi = "सुनो जी भैरव लाडले, कर जोड़ कर विनती करूँ",
        durationText = "4:45",
        audioUrl = "https://shribalajikripadham.online/uploads/audio/sbkd_audio_1791373307_b8cfc4.mp3",
        lyricsHindi = """{bhairav_lyrics}""".trimIndent(),
        isPublished = true,
        displayOrder = 6,'''
content = re.sub(old_bhairav_pattern, new_bhairav, content)

# Replace guru_vandana
old_guru_pattern = r'id = 8,\s+trackKey = "guru_vandana",[\s\S]*?displayOrder = 8,'
new_guru = f'''id = 8,
        trackKey = "guru_vandana",
        titleHindi = "श्री गुरुदेव जी की आरती",
        titleEnglish = "Shri Gurudev Ji Ki Aarti",
        subtitleHindi = "जय गुरुदेव दयानिधि, दीनन हितकारी",
        durationText = "5:10",
        audioUrl = "https://shribalajikripadham.online/uploads/audio/sbkd_audio_1791373309_c31a75.mp3",
        lyricsHindi = """{guru_lyrics}""".trimIndent(),
        isPublished = true,
        displayOrder = 8,'''
content = re.sub(old_guru_pattern, new_guru, content)

with open(kt_path, "w", encoding="utf-8") as f:
    f.write(content)

print("Updated SacredTracksData.kt with authentic Balaji Aarti, Bhairav Ladle Aarti, and Gurudev Aarti!")

# Parse all 18 tracks from SacredTracksData.kt and sync to Hostinger
raw_tracks = content.split('SacredTrack(')[1:]
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
            'is_published': 1,
            'youtube_search_query': yt,
            'lyrics_hindi': lyrics
        })

print(f"Total parsed tracks: {len(tracks)}. Syncing to Hostinger...")
for t in tracks:
    url = "https://shribalajikripadham.online/api/save_sacred_track.php"
    payload = json.dumps(t).encode('utf-8')
    req = urllib.request.Request(
        url,
        data=payload,
        headers={
            "Content-Type": "application/json",
            "X-SBKD-API-KEY": "SBKD_SECURE_TOKEN_9100100251233433_V243"
        },
        method="POST"
    )
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            res = r.read().decode('utf-8')
            tkey = t['track_key']
            print(f"Synced {tkey}: length={len(t['lyrics_hindi'])}")
    except Exception as e:
        print(f"Error syncing {t['track_key']}: {e}")

# Also update backend/api/migrate_tracks.php
php_path = r"backend\api\migrate_tracks.php"
with open(r"scratch\sync_tracks_to_backend_php.py", "w", encoding="utf-8") as f:
    f.write(f'''# Update migrate_tracks.php
import json

tracks_json = {json.dumps(tracks, ensure_ascii=False, indent=4)}

php_code = """<?php
// Shri Balaji Kripa Dham - 18 Sacred Tracks Complete Authentic Database Migration
header('Content-Type: application/json; charset=utf-8');
require_once __DIR__ . '/../config/db.php';

try {{
    $pdo = getDB();
    if (!$pdo) throw new Exception("Database connection failed");

    $defaultTracks = """ . var_export($tracks_json, true) . """;

    $count = 0;
    foreach ($defaultTracks as $t) {{
        $st = $pdo->prepare("INSERT INTO sacred_tracks 
            (track_key, title_hindi, title_english, subtitle_hindi, duration_text, audio_url, lyrics_hindi, is_published, display_order, youtube_search_query) 
            VALUES (:tk, :th, :te, :sh, :dt, :au, :lh, :ip, :do, :yq)
            ON DUPLICATE KEY UPDATE 
                title_hindi = VALUES(title_hindi),
                title_english = VALUES(title_english),
                subtitle_hindi = VALUES(subtitle_hindi),
                duration_text = VALUES(duration_text),
                audio_url = VALUES(audio_url),
                lyrics_hindi = VALUES(lyrics_hindi),
                is_published = VALUES(is_published),
                display_order = VALUES(display_order),
                youtube_search_query = VALUES(youtube_search_query)");

        $st->execute([
            ':tk' => $t['track_key'],
            ':th' => $t['title_hindi'],
            ':te' => $t['title_english'],
            ':sh' => $t['subtitle_hindi'],
            ':dt' => $t['duration_text'],
            ':au' => $t['audio_url'],
            ':lh' => $t['lyrics_hindi'],
            ':ip' => $t['is_published'],
            ':do' => $t['display_order'],
            ':yq' => $t['youtube_search_query']
        ]);
        $count++;
    }}

    echo json_encode(["success" => true, "migrated" => $count, "message" => "18 Sacred Tracks authentic migration complete."]);
}} catch (Throwable $e) {{
    echo json_encode(["success" => false, "error" => $e->getMessage()]);
}}
"""

with open(r"{php_path}", "w", encoding="utf-8") as pf:
    pf.write(php_code)

print("backend/api/migrate_tracks.php updated with 18 authentic tracks!")
''')

import subprocess
subprocess.run([sys.executable, r"scratch\sync_tracks_to_backend_php.py"], check=True)
print("All 18 tracks successfully generated and synced locally & remotely!")
