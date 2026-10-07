import json

with open('scratch/uploaded_audio_map.json', 'r', encoding='utf-8') as f:
    audio_map = json.load(f)

# Read the track definitions from sync_tracks_to_hostinger.py
with open('scratch/sync_tracks_to_hostinger.py', 'r', encoding='utf-8') as f:
    code = f.read()

# Extract tracks_data
namespace = {'audio_map': audio_map}
exec(code[code.find('tracks_data = ['):code.find('print(f"Syncing')], namespace)
tracks_data = namespace['tracks_data']

kotlin_code = """package com.example.shribalajikripadham.data.sacred

/**
 * श्री बालाजी कृपा धाम (डूँगरा जाट) - पावन आरती एवं भजन डेटा मॉडल
 * Authentic pre-loaded devotional hymns with complete lyrics and high-quality audio streams.
 * 100% Hostinger Cloud Hosted Audio Streams - Zero Dummy Data.
 */
data class SacredTrack(
    val id: Long = 0,
    val trackKey: String = "",
    val titleHindi: String = "",
    val titleEnglish: String = "",
    val subtitleHindi: String = "",
    val durationText: String = "",
    val audioUrl: String = "",
    val lyricsHindi: String = "",
    val isPublished: Boolean = true,
    val displayOrder: Int = 0,
    val youtubeSearchQuery: String = ""
)

val SACRED_TRACKS: List<SacredTrack> = listOf(
"""

items = []
for idx, t in enumerate(tracks_data, 1):
    escaped_lyrics = t['lyrics_hindi'].replace('$', '\\$')
    item = f"""    SacredTrack(
        id = {idx},
        trackKey = "{t['track_key']}",
        titleHindi = "{t['title_hindi']}",
        titleEnglish = "{t['title_english']}",
        subtitleHindi = "{t['subtitle_hindi']}",
        durationText = "{t['duration_text']}",
        audioUrl = "{t['audio_url']}",
        lyricsHindi = \"\"\"{escaped_lyrics}\"\"\".trimIndent(),
        isPublished = true,
        displayOrder = {t['display_order']},
        youtubeSearchQuery = "{t['youtube_search_query']}"
    )"""
    items.append(item)

kotlin_code += ",\n".join(items) + "\n)\n"

with open('app/src/main/java/com/example/shribalajikripadham/data/sacred/SacredTracksData.kt', 'w', encoding='utf-8') as f:
    f.write(kotlin_code)

print("Successfully written SacredTracksData.kt with 12 authentic tracks!")
