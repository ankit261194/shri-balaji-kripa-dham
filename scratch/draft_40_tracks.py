import re
import json

# Let's inspect the existing 23 tracks in SacredTracksData.kt
with open("app/src/main/java/com/example/shribalajikripadham/data/sacred/SacredTracksData.kt", "r", encoding="utf-8") as f:
    code = f.read()

# Updated audio URLs for tracks 1-23
updated_urls = {
    "pretraj_chalisa": "https://shribalajikripadham.online/uploads/audio/sbkd_audio_1791441293_a95a78.mp3",
    "ramayan_aarti": "https://shribalajikripadham.online/uploads/audio/sbkd_audio_1791440726_64a083.mp3",
    "jagdish_aarti": "https://shribalajikripadham.online/uploads/audio/sbkd_audio_1791440335_30264b.mp3",
    "laxmi_aarti": "https://shribalajikripadham.online/uploads/audio/sbkd_audio_1791439520_2cf896.mp3",
    "kunj_bihari_aarti": "https://shribalajikripadham.online/uploads/audio/sbkd_audio_1791440552_161c5b.mp3",
    "khatu_shyam_aarti": "https://shribalajikripadham.online/uploads/audio/sbkd_audio_1791441343_136194.mp3",
    "satyanarayan_aarti": "https://shribalajikripadham.online/uploads/audio/sbkd_audio_1791441351_3ee6b3.mp3",
    "durga_chalisa": "https://shribalajikripadham.online/uploads/audio/sbkd_audio_1791440367_cec009.mp3",
    "vindhweshwari_chalisa": "https://shribalajikripadham.online/uploads/audio/sbkd_audio_1791440794_bac771.mp3",
    "hanuman_veer_raksha_mantra": "https://shribalajikripadham.online/uploads/audio/sbkd_audio_1791441216_bc947e.mp3",
    "bhajan_meri_naav_chali": "https://shribalajikripadham.online/uploads/audio/sbkd_audio_1791441369_309dbf.mp3",
    "bhajan_aaj_mangalwar_hai": "https://shribalajikripadham.online/uploads/audio/sbkd_audio_1791441389_2564d8.mp3"
}

# The 17 Durga Saptashati tracks
saptashati_tracks = [
    {
        "trackKey": "durga_saptashati_kavach",
        "titleHindi": "श्री देवी कवचम्",
        "titleEnglish": "Shri Devi Kavacham",
        "subtitleHindi": "मार्कण्डेय पुराणोक्त • नवदुर्गा रक्षा कवच",
        "durationText": "16:07",
        "audioUrl": "https://archive.org/download/DeviMahatmyamRecitationAudio/05%20Devi%20Kavacham.mp3",
        "youtubeSearchQuery": "Shri Devi Kavacham Anuradha Paudwal"
    },
    {
        "trackKey": "durga_saptashati_argala",
        "titleHindi": "श्री अर्गला स्तोत्रम्",
        "titleEnglish": "Shri Argala Stotram",
        "subtitleHindi": "जयन्ती मङ्गला काली • रूपं देहि जयं देहि",
        "durationText": "6:46",
        "audioUrl": "https://archive.org/download/DeviMahatmyamRecitationAudio/06%20Argala%20Stotram.mp3",
        "youtubeSearchQuery": "Shri Argala Stotram Anuradha Paudwal"
    },
    {
        "trackKey": "durga_saptashati_keelak",
        "titleHindi": "श्री कीलक स्तोत्रम्",
        "titleEnglish": "Shri Keelakam Stotram",
        "subtitleHindi": "महामंत्र निष्कीलन • सर्व कल्याणकारी पाठ",
        "durationText": "4:25",
        "audioUrl": "https://archive.org/download/DeviMahatmyamRecitationAudio/07%20Keelakam.mp3",
        "youtubeSearchQuery": "Shri Keelak Stotram Anuradha Paudwal"
    },
    {
        "trackKey": "durga_saptashati_ch1",
        "titleHindi": "दुर्गा सप्तशती - प्रथमोऽध्यायः",
        "titleEnglish": "Durga Saptashati Chapter 1",
        "subtitleHindi": "मधु-कैटभ वध • महाकाली प्रादुर्भाव",
        "durationText": "21:41",
        "audioUrl": "https://archive.org/download/DeviMahatmyamRecitationAudio/Chapter%201%20Pradamodhyayha.mp3",
        "youtubeSearchQuery": "Durga Saptashati Chapter 1 Prathamo Adhyaya"
    },
    {
        "trackKey": "durga_saptashati_ch2",
        "titleHindi": "दुर्गा सप्तशती - द्वितीयोऽध्यायः",
        "titleEnglish": "Durga Saptashati Chapter 2",
        "subtitleHindi": "महिषासुर की सेना का संहार • मध्यम चरित्र",
        "durationText": "18:13",
        "audioUrl": "https://archive.org/download/DeviMahatmyamRecitationAudio/Chapter%202%20Divitiyodhyayaha.mp3",
        "youtubeSearchQuery": "Durga Saptashati Chapter 2 Dwitiyo Adhyaya"
    },
    {
        "trackKey": "durga_saptashati_ch3",
        "titleHindi": "दुर्गा सप्तशती - तृतीयोऽध्यायः",
        "titleEnglish": "Durga Saptashati Chapter 3",
        "subtitleHindi": "महिषासुर वध • महालक्ष्मी पराक्रम",
        "durationText": "10:51",
        "audioUrl": "https://archive.org/download/DeviMahatmyamRecitationAudio/Chapter%203%20Tritiyodhyayaha.mp3",
        "youtubeSearchQuery": "Durga Saptashati Chapter 3 Tritiyo Adhyaya"
    },
    {
        "trackKey": "durga_saptashati_ch4",
        "titleHindi": "दुर्गा सप्तशती - चतुर्थोऽध्यायः",
        "titleEnglish": "Durga Saptashati Chapter 4",
        "subtitleHindi": "इन्द्रादि देवताओं द्वारा देवी-स्तुति (शक्रादि स्तुति)",
        "durationText": "13:18",
        "audioUrl": "https://archive.org/download/DeviMahatmyamRecitationAudio/Chapter%204%20Chaturthodhayayaha.mp3",
        "youtubeSearchQuery": "Durga Saptashati Chapter 4 Chaturtho Adhyaya"
    },
    {
        "trackKey": "durga_saptashati_ch5",
        "titleHindi": "दुर्गा सप्तशती - पञ्चमोऽध्यायः",
        "titleEnglish": "Durga Saptashati Chapter 5",
        "subtitleHindi": "देवताओं द्वारा अपराजिता स्तुति व चण्ड-मुण्ड वार्ता",
        "durationText": "28:09",
        "audioUrl": "https://archive.org/download/DeviMahatmyamRecitationAudio/Chapter%205%20Panchamodhyayaha.mp3",
        "youtubeSearchQuery": "Durga Saptashati Chapter 5 Panchamo Adhyaya"
    },
    {
        "trackKey": "durga_saptashati_ch6",
        "titleHindi": "दुर्गा सप्तशती - षष्ठोऽध्यायः",
        "titleEnglish": "Durga Saptashati Chapter 6",
        "subtitleHindi": "धूम्रलोचन वध • हुंकार से दैत्य भस्म",
        "durationText": "6:21",
        "audioUrl": "https://archive.org/download/DeviMahatmyamRecitationAudio/Chapter%206%20Shashtodhyayaha.mp3",
        "youtubeSearchQuery": "Durga Saptashati Chapter 6 Shashtho Adhyaya"
    },
    {
        "trackKey": "durga_saptashati_ch7",
        "titleHindi": "दुर्गा सप्तशती - सप्तमोऽध्यायः",
        "titleEnglish": "Durga Saptashati Chapter 7",
        "subtitleHindi": "चण्ड और मुण्ड वध • चामुण्डा देवी प्राकट्य",
        "durationText": "7:25",
        "audioUrl": "https://archive.org/download/DeviMahatmyamRecitationAudio/Chapter%207%20Saptamodhyayaha.mp3",
        "youtubeSearchQuery": "Durga Saptashati Chapter 7 Saptamo Adhyaya"
    },
    {
        "trackKey": "durga_saptashati_ch8",
        "titleHindi": "दुर्गा सप्तशती - अष्टमोऽध्यायः",
        "titleEnglish": "Durga Saptashati Chapter 8",
        "subtitleHindi": "रक्तबीज वध • मातृका शक्तियों का संग्राम",
        "durationText": "15:57",
        "audioUrl": "https://archive.org/download/DeviMahatmyamRecitationAudio/Chapter%208%20Ashtamodhyayaha.mp3",
        "youtubeSearchQuery": "Durga Saptashati Chapter 8 Ashtamo Adhyaya"
    },
    {
        "trackKey": "durga_saptashati_ch9",
        "titleHindi": "दुर्गा सप्तशती - नवमोऽध्यायः",
        "titleEnglish": "Durga Saptashati Chapter 9",
        "subtitleHindi": "निशुम्भ वध • भगवती का प्रचण्ड पराक्रम",
        "durationText": "10:33",
        "audioUrl": "https://archive.org/download/DeviMahatmyamRecitationAudio/Chapter%209%20Navamodhyayaha.mp3",
        "youtubeSearchQuery": "Durga Saptashati Chapter 9 Navamo Adhyaya"
    },
    {
        "trackKey": "durga_saptashati_ch10",
        "titleHindi": "दुर्गा सप्तशती - दशमोऽध्यायः",
        "titleEnglish": "Durga Saptashati Chapter 10",
        "subtitleHindi": "शुम्भ वध • दैत्यराज का अंत",
        "durationText": "7:55",
        "audioUrl": "https://archive.org/download/DeviMahatmyamRecitationAudio/Chapter10%20Dasamodhyayaha.mp3",
        "youtubeSearchQuery": "Durga Saptashati Chapter 10 Dashamo Adhyaya"
    },
    {
        "trackKey": "durga_saptashati_ch11",
        "titleHindi": "दुर्गा सप्तशती - एकादशोऽध्यायः",
        "titleEnglish": "Durga Saptashati Chapter 11",
        "subtitleHindi": "देवताओं द्वारा नारायणी स्तुति एवं वरदान",
        "durationText": "14:30",
        "audioUrl": "https://archive.org/download/DeviMahatmyamRecitationAudio/Chapter11%20Ekadhasadhyayaha.mp3",
        "youtubeSearchQuery": "Durga Saptashati Chapter 11 Ekadasho Adhyaya"
    },
    {
        "trackKey": "durga_saptashati_ch12",
        "titleHindi": "दुर्गा सप्तशती - द्वादशोऽध्यायः",
        "titleEnglish": "Durga Saptashati Chapter 12",
        "subtitleHindi": "देवी चरित्रों के पाठ का महात्म्य एवं फलश्रुति",
        "durationText": "11:11",
        "audioUrl": "https://archive.org/download/DeviMahatmyamRecitationAudio/Chapter12%20Dwadhasodhyayaha.mp3",
        "youtubeSearchQuery": "Durga Saptashati Chapter 12 Dwadasho Adhyaya"
    },
    {
        "trackKey": "durga_saptashati_ch13",
        "titleHindi": "दुर्गा सप्तशती - त्रयोदशोऽध्यायः",
        "titleEnglish": "Durga Saptashati Chapter 13",
        "subtitleHindi": "राजा सुरथ और समाधि वैश्य को वरदान प्राप्ति",
        "durationText": "5:50",
        "audioUrl": "https://archive.org/download/DeviMahatmyamRecitationAudio/Chapter13%20Triodasadhyayaha.mp3",
        "youtubeSearchQuery": "Durga Saptashati Chapter 13 Trayodasho Adhyaya"
    },
    {
        "trackKey": "durga_saptashati_kshama",
        "titleHindi": "श्री देवी अपराध क्षमापन स्तोत्रम्",
        "titleEnglish": "Shri Devi Kshama Prarthana",
        "subtitleHindi": "अपराधसहस्त्राणि क्रियन्तेऽहर्निशं मया • क्षमा स्तोत्र",
        "durationText": "1:58",
        "audioUrl": "https://archive.org/download/DeviMahatmyamRecitationAudio/Kshama%20Prathana%20Stotram.mp3",
        "youtubeSearchQuery": "Devi Aparadha Kshamapana Stotram"
    }
]

print("Script template ready.")
