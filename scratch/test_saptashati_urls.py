import urllib.request

urls = [
    ('Kavach', 'https://archive.org/download/DeviMahatmyamRecitationAudio/05%20Devi%20Kavacham.mp3'),
    ('Argala', 'https://archive.org/download/DeviMahatmyamRecitationAudio/06%20Argala%20Stotram.mp3'),
    ('Keelak', 'https://archive.org/download/DeviMahatmyamRecitationAudio/07%20Keelakam.mp3'),
    ('Ch1', 'https://archive.org/download/DeviMahatmyamRecitationAudio/Chapter%201%20Pradamodhyayha.mp3'),
    ('Ch2', 'https://archive.org/download/DeviMahatmyamRecitationAudio/Chapter%202%20Divitiyodhyayaha.mp3'),
    ('Ch3', 'https://archive.org/download/DeviMahatmyamRecitationAudio/Chapter%203%20Tritiyodhyayaha.mp3'),
    ('Ch4', 'https://archive.org/download/DeviMahatmyamRecitationAudio/Chapter%204%20Chaturthodhayayaha.mp3'),
    ('Ch5', 'https://archive.org/download/DeviMahatmyamRecitationAudio/Chapter%205%20Panchamodhyayaha.mp3'),
    ('Ch6', 'https://archive.org/download/DeviMahatmyamRecitationAudio/Chapter%206%20Shashtodhyayaha.mp3'),
    ('Ch7', 'https://archive.org/download/DeviMahatmyamRecitationAudio/Chapter%207%20Saptamodhyayaha.mp3'),
    ('Ch8', 'https://archive.org/download/DeviMahatmyamRecitationAudio/Chapter%208%20Ashtamodhyayaha.mp3'),
    ('Ch9', 'https://archive.org/download/DeviMahatmyamRecitationAudio/Chapter%209%20Navamodhyayaha.mp3'),
    ('Ch10', 'https://archive.org/download/DeviMahatmyamRecitationAudio/Chapter10%20Dasamodhyayaha.mp3'),
    ('Ch11', 'https://archive.org/download/DeviMahatmyamRecitationAudio/Chapter11%20Ekadhasadhyayaha.mp3'),
    ('Ch12', 'https://archive.org/download/DeviMahatmyamRecitationAudio/Chapter12%20Dwadhasodhyayaha.mp3'),
    ('Ch13', 'https://archive.org/download/DeviMahatmyamRecitationAudio/Chapter13%20Triodasadhyayaha.mp3'),
    ('Kshama', 'https://archive.org/download/DeviMahatmyamRecitationAudio/Kshama%20Prathana%20Stotram.mp3')
]

for label, u in urls:
    try:
        req = urllib.request.Request(u, method='HEAD', headers={'User-Agent': 'Mozilla/5.0'})
        with urllib.request.urlopen(req, timeout=10) as r:
            cl = r.headers.get("Content-Length", "?")
            print(f"{label}: STATUS {r.status}, SIZE {cl}")
    except Exception as e:
        print(f"{label}: ERROR {e}")
