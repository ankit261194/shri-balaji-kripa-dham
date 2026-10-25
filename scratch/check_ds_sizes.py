import sys
import urllib.request

sys.stdout.reconfigure(encoding='utf-8')

ds_files = [
    ("durga_saptashati_kavach", "https://archive.org/download/DeviMahatmyamRecitationAudio/05%20Devi%20Kavacham.mp3"),
    ("durga_saptashati_argala", "https://archive.org/download/DeviMahatmyamRecitationAudio/06%20Argala%20Stotram.mp3"),
    ("durga_saptashati_keelak", "https://archive.org/download/DeviMahatmyamRecitationAudio/07%20Keelakam.mp3"),
    ("durga_saptashati_ch1", "https://archive.org/download/DeviMahatmyamRecitationAudio/Chapter%201%20Pradamodhyayha.mp3"),
    ("durga_saptashati_ch2", "https://archive.org/download/DeviMahatmyamRecitationAudio/Chapter%202%20Divitiyodhyayaha.mp3"),
    ("durga_saptashati_ch3", "https://archive.org/download/DeviMahatmyamRecitationAudio/Chapter%203%20Tritiyodhyayaha.mp3"),
    ("durga_saptashati_ch4", "https://archive.org/download/DeviMahatmyamRecitationAudio/Chapter%204%20Chaturthodhayayaha.mp3"),
    ("durga_saptashati_ch5", "https://archive.org/download/DeviMahatmyamRecitationAudio/Chapter%205%20Panchamodhyayaha.mp3"),
    ("durga_saptashati_ch6", "https://archive.org/download/DeviMahatmyamRecitationAudio/Chapter%206%20Shashtodhyayaha.mp3"),
    ("durga_saptashati_ch7", "https://archive.org/download/DeviMahatmyamRecitationAudio/Chapter%207%20Saptamodhyayaha.mp3"),
    ("durga_saptashati_ch8", "https://archive.org/download/DeviMahatmyamRecitationAudio/Chapter%208%20Ashtamodhyayaha.mp3"),
    ("durga_saptashati_ch9", "https://archive.org/download/DeviMahatmyamRecitationAudio/Chapter%209%20Navamodhyayaha.mp3"),
    ("durga_saptashati_ch10", "https://archive.org/download/DeviMahatmyamRecitationAudio/Chapter10%20Dasamodhyayaha.mp3"),
    ("durga_saptashati_ch11", "https://archive.org/download/DeviMahatmyamRecitationAudio/Chapter11%20Ekadhasadhyayaha.mp3"),
    ("durga_saptashati_ch12", "https://archive.org/download/DeviMahatmyamRecitationAudio/Chapter12%20Dwadhasodhyayaha.mp3"),
    ("durga_saptashati_ch13", "https://archive.org/download/DeviMahatmyamRecitationAudio/Chapter13%20Triodasadhyayaha.mp3"),
    ("durga_saptashati_kshama", "https://archive.org/download/DeviMahatmyamRecitationAudio/Kshama%20Prathana%20Stotram.mp3")
]

total_size = 0
for key, url in ds_files:
    try:
        req = urllib.request.Request(url, method='HEAD', headers={'User-Agent': 'Mozilla/5.0'})
        with urllib.request.urlopen(req, timeout=15) as resp:
            sz = int(resp.headers.get('Content-Length', 0))
            total_size += sz
            print(f"{key:<25}: {sz / (1024*1024):6.2f} MB")
    except Exception as e:
        print(f"{key:<25}: Error {e}")

print(f"\nTotal Durga Saptashati Audio Size: {total_size / (1024*1024):.2f} MB")
