import urllib.request

urls = [
    ("1. Hanuman Chalisa", "https://shribalajikripadham.online/uploads/audio/sbkd_audio_1791373303_b758f0.mp3"),
    ("2. Hanuman Lala Aarti", "https://shribalajikripadham.online/uploads/audio/sbkd_audio_1791373304_1fc6aa.mp3"),
    ("3. Balaji Aarti", "https://shribalajikripadham.online/uploads/audio/sbkd_audio_1791373307_1a7f1b.mp3"),
    ("4. Sankat Mochan", "https://shribalajikripadham.online/uploads/audio/sbkd_audio_1791373304_b22622.mp3"),
    ("5. Bajrang Baan", "https://shribalajikripadham.online/uploads/audio/sbkd_audio_1791373305_f87240.mp3"),
    ("6. Bhairav Ladle Aarti", "https://shribalajikripadham.online/uploads/audio/sbkd_audio_1791373307_b8cfc4.mp3"),
    ("7. Pretraj Chalisa", "https://shribalajikripadham.online/uploads/audio/sbkd_audio_1791441293_a95a78.mp3"),
    ("8. Guru Vandana", "https://shribalajikripadham.online/uploads/audio/sbkd_audio_1791373309_c31a75.mp3"),
    ("9. Ram Stuti", "https://shribalajikripadham.online/uploads/audio/sbkd_audio_1791373306_76412f.mp3"),
    ("10. Ganesh Aarti", "https://shribalajikripadham.online/uploads/audio/sbkd_audio_1791373310_682644.mp3"),
    ("11. Durga Aarti", "https://shribalajikripadham.online/uploads/audio/sbkd_audio_1791373310_b59376.mp3"),
    ("12. Shiv Aarti", "https://shribalajikripadham.online/uploads/audio/sbkd_audio_1791373311_1182ec.mp3"),
    ("13. Ramayan Aarti", "https://shribalajikripadham.online/uploads/audio/sbkd_audio_1791440726_64a083.mp3"),
    ("14. Jagdish Aarti", "https://shribalajikripadham.online/uploads/audio/sbkd_audio_1791440335_30264b.mp3"),
    ("15. Laxmi Aarti", "https://shribalajikripadham.online/uploads/audio/sbkd_audio_1791439520_2cf896.mp3"),
    ("16. Kunj Bihari Aarti", "https://shribalajikripadham.online/uploads/audio/sbkd_audio_1791440552_161c5b.mp3"),
    ("17. Khatu Shyam Aarti", "https://shribalajikripadham.online/uploads/audio/sbkd_audio_1791441343_136194.mp3"),
    ("18. Satyanarayan Aarti", "https://shribalajikripadham.online/uploads/audio/sbkd_audio_1791441351_3ee6b3.mp3"),
    ("19. Durga Chalisa", "https://shribalajikripadham.online/uploads/audio/sbkd_audio_1791440367_cec009.mp3"),
    ("20. Vindhweshwari Chalisa", "https://shribalajikripadham.online/uploads/audio/sbkd_audio_1791440794_bac771.mp3"),
    ("21. Veer Shabar Mantra", "https://shribalajikripadham.online/uploads/audio/sbkd_audio_1791441216_bc947e.mp3"),
    ("22. Meri Naav Chali", "https://shribalajikripadham.online/uploads/audio/sbkd_audio_1791441369_309dbf.mp3"),
    ("23. Aaj Mangalwar Hai", "https://shribalajikripadham.online/uploads/audio/sbkd_audio_1791441389_2564d8.mp3"),
    ("24. Saptashati Kavach", "https://archive.org/download/DeviMahatmyamRecitationAudio/05%20Devi%20Kavacham.mp3"),
    ("25. Saptashati Argala", "https://archive.org/download/DeviMahatmyamRecitationAudio/06%20Argala%20Stotram.mp3"),
    ("26. Saptashati Keelak", "https://archive.org/download/DeviMahatmyamRecitationAudio/07%20Keelakam.mp3"),
    ("27. Saptashati Ch 1", "https://archive.org/download/DeviMahatmyamRecitationAudio/Chapter%201%20Pradamodhyayha.mp3"),
    ("28. Saptashati Ch 2", "https://archive.org/download/DeviMahatmyamRecitationAudio/Chapter%202%20Divitiyodhyayaha.mp3"),
    ("29. Saptashati Ch 3", "https://archive.org/download/DeviMahatmyamRecitationAudio/Chapter%203%20Tritiyodhyayaha.mp3"),
    ("30. Saptashati Ch 4", "https://archive.org/download/DeviMahatmyamRecitationAudio/Chapter%204%20Chaturthodhayayaha.mp3"),
    ("31. Saptashati Ch 5", "https://archive.org/download/DeviMahatmyamRecitationAudio/Chapter%205%20Panchamodhyayaha.mp3"),
    ("32. Saptashati Ch 6", "https://archive.org/download/DeviMahatmyamRecitationAudio/Chapter%206%20Shashtodhyayaha.mp3"),
    ("33. Saptashati Ch 7", "https://archive.org/download/DeviMahatmyamRecitationAudio/Chapter%207%20Saptamodhyayaha.mp3"),
    ("34. Saptashati Ch 8", "https://archive.org/download/DeviMahatmyamRecitationAudio/Chapter%208%20Ashtamodhyayaha.mp3"),
    ("35. Saptashati Ch 9", "https://archive.org/download/DeviMahatmyamRecitationAudio/Chapter%209%20Navamodhyayaha.mp3"),
    ("36. Saptashati Ch 10", "https://archive.org/download/DeviMahatmyamRecitationAudio/Chapter10%20Dasamodhyayaha.mp3"),
    ("37. Saptashati Ch 11", "https://archive.org/download/DeviMahatmyamRecitationAudio/Chapter11%20Ekadhasadhyayaha.mp3"),
    ("38. Saptashati Ch 12", "https://archive.org/download/DeviMahatmyamRecitationAudio/Chapter12%20Dwadhasodhyayaha.mp3"),
    ("39. Saptashati Ch 13", "https://archive.org/download/DeviMahatmyamRecitationAudio/Chapter13%20Triodasadhyayaha.mp3"),
    ("40. Saptashati Kshama", "https://archive.org/download/DeviMahatmyamRecitationAudio/Kshama%20Prathana%20Stotram.mp3")
]

all_passed = True
for name, u in urls:
    try:
        req = urllib.request.Request(u, headers={'User-Agent': 'Mozilla/5.0'})
        if 'archive.org' in u:
            req.headers['Range'] = 'bytes=0-10'
        with urllib.request.urlopen(req, timeout=10) as resp:
            if resp.status not in (200, 206):
                print(f"FAILED {name}: status {resp.status}")
                all_passed = False
            else:
                pass
    except Exception as e:
        print(f"ERROR {name}: {e}")
        all_passed = False

if all_passed:
    print("ALL 40 AUDIO TRACKS ARE 100% OPERATIONAL, VERIFIED AND LIVE!")
